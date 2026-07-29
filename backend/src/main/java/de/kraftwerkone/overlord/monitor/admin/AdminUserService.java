package de.kraftwerkone.overlord.monitor.admin;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.AppUserRepository;
import de.kraftwerkone.overlord.monitor.security.MandantRepository;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anlegen eines Kontos — <b>und nur das</b>.
 *
 * <p>Auflisten, Sperren, Rollenwechsel, Zuruecksetzen und Loeschen gehoeren zu Schritt 9. Hier
 * entsteht ausschliesslich das Anlegen, weil ohne mindestens einen Nutzer der Rolle {@code MANDANT}
 * nichts testbar waere: Der Nachweis, dass ein fremder Mandant dieselbe Antwort liefert wie eine
 * erfundene ID, braucht genau so einen Nutzer.
 *
 * <p>Dieser Vorgang ersetzt den urspruenglich geplanten Migrationslauf ueber die
 * Alt-Benutzertabelle. Die wird <b>nicht</b> uebernommen: {@code GlassfishDB.User} bleibt dauerhaft
 * lesbar, eine Uebernahme waere also nur ein vorgezogener {@code SELECT}; alle 36 Konten waeren
 * gesperrt und ohne Passwort gestartet und einzeln freizuschalten gewesen — derselbe Aufwand wie
 * neu anlegen, nur mit toten Zeilen als Zwischenschritt.
 */
@Service
public class AdminUserService {

  /** Mindestlaenge des Einmalpassworts — dieselbe wie bei der Passwortaenderung. */
  static final int MINDESTLAENGE = 12;

  /**
   * Hoechstlaenge des Benutzernamens.
   *
   * <p><b>Die Grenze kommt nicht von {@code app_user.username}</b> (190 Zeichen), sondern von
   * {@code SPRING_SESSION.PRINCIPAL_NAME} aus der Spring-Session-Distribution: 100 Zeichen. Ein
   * laengerer Name liesse sich anlegen, koennte sich aber nie anmelden — das Konto waere von Anfang
   * an tot, und der Fehler traete erst beim ersten Anmeldeversuch als Datenbankfehler auf.
   */
  static final int MAX_BENUTZERNAME = 100;

  private final AppUserRepository appUserRepository;
  private final MandantRepository mandantRepository;
  private final PasswordEncoder passwortKodierer;
  private final AuditLogWriter auditLogWriter;
  private final Clock systemClock;

  AdminUserService(
      AppUserRepository appUserRepository,
      MandantRepository mandantRepository,
      PasswordEncoder passwortKodierer,
      AuditLogWriter auditLogWriter,
      @Qualifier("systemClock") Clock systemClock) {
    this.appUserRepository = appUserRepository;
    this.mandantRepository = mandantRepository;
    this.passwortKodierer = passwortKodierer;
    this.auditLogWriter = auditLogWriter;
    this.systemClock = systemClock;
  }

  /**
   * Legt ein aktives Konto mit Aenderungszwang an.
   *
   * <p>Die Mandanten-ID wird gegen {@code GlassfishDB.Mandant} geprueft — das ist die <b>zweite und
   * letzte</b> Ausnahme von Regel M1. Sie ist zulaessig, weil hier ein Konto <i>definiert</i> und
   * kein Datenausschnitt <i>abgefragt</i> wird; welchen Mandanten der anlegende Admin gerade aktiv
   * hat, sagt nichts darueber aus, fuer wen das neue Konto gilt. Anders als beim Wechsel darf hier
   * auch „unbekannt" als {@code 404} erscheinen: Ein ADMIN kennt die Mandantenliste ohnehin.
   */
  @Transactional
  public AngelegterNutzerResponse legeAn(
      AngemeldeterNutzer admin,
      String username,
      String rolleText,
      String mandantId,
      String einmalpasswort,
      String ip) {

    Rolle rolle = rolleAus(rolleText);
    pruefeBenutzername(username);
    if (einmalpasswort == null || einmalpasswort.length() < MINDESTLAENGE) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "passwort-zu-kurz",
          "Passwort zu kurz",
          "Das Einmalpasswort braucht mindestens " + MINDESTLAENGE + " Zeichen.");
    }
    if (!mandantRepository.existiert(mandantId)) {
      throw new RessourceNichtGefundenException("Unbekannte MandantID beim Anlegen eines Kontos");
    }
    if (appUserRepository.existiertBenutzername(username)) {
      throw benutzernameVergeben();
    }

    LocalDateTime jetztUtc = LocalDateTime.ofInstant(systemClock.instant(), ZoneOffset.UTC);
    long id;
    try {
      id =
          appUserRepository.legeAn(
              username, passwortKodierer.encode(einmalpasswort), rolle, true, true, jetztUtc);
    } catch (DuplicateKeyException ex) {
      // Der eindeutige Index ist die Wahrheit, die Vorabpruefung nur die freundliche Antwort.
      // Ueberschrieben wird nie.
      throw benutzernameVergeben();
    }
    // Auch fuer ADMIN gespeichert: Die Zuordnung hat dort keine Wirkung auf die waehlbare Menge,
    // faellt aber bei einer spaeteren Herabstufung auf MANDANT nicht ins Leere — und eine
    // stillschweigend verworfene Eingabe waere schlechter als eine wirkungslose Zeile.
    appUserRepository.ordneMandantZu(id, mandantId);

    auditLogWriter.schreibe(
        new AuditEvent(
            AuditEventType.NUTZER_ANGELEGT,
            admin.id(),
            admin.username(),
            mandantId,
            "app_user",
            String.valueOf(id),
            ip,
            // Niemals das Passwort — weder hier noch im Anwendungsprotokoll.
            "angelegt: " + username + ", Rolle " + rolle.name() + ", Mandant " + mandantId));
    return new AngelegterNutzerResponse(id, username, rolle.name(), mandantId);
  }

  /** Auch vom Bootstrap benutzt — die Grenze gilt fuer jedes Konto, nicht nur fuer angelegte. */
  static void pruefeBenutzername(String username) {
    if (username == null || username.length() > MAX_BENUTZERNAME) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "benutzername-zu-lang",
          "Benutzername zu lang",
          "Der Benutzername darf hoechstens " + MAX_BENUTZERNAME + " Zeichen haben.");
    }
  }

  private static FachlicheAusnahme benutzernameVergeben() {
    return new FachlicheAusnahme(
        HttpStatus.CONFLICT,
        "benutzername-vergeben",
        "Benutzername vergeben",
        "Diesen Benutzernamen gibt es bereits. Waehle einen anderen.");
  }

  private static Rolle rolleAus(String rolleText) {
    try {
      return Rolle.valueOf(rolleText);
    } catch (IllegalArgumentException | NullPointerException ex) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "unbekannte-rolle",
          "Unbekannte Rolle",
          "Gueltige Rollen sind MANDANT und ADMIN.");
    }
  }
}
