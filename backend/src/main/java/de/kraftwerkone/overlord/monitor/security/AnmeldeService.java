package de.kraftwerkone.overlord.monitor.security;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.common.error.AnmeldungAbgelehntException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Prueft eine Anmeldung und fuehrt Fehlversuchszaehler und Sperre.
 *
 * <p><b>Die Reihenfolge der Pruefungen ist der eigentliche Inhalt dieser Klasse</b> und keine
 * Geschmacksfrage:
 *
 * <ol>
 *   <li><b>IP-Begrenzung</b> zuerst — sie kostet nichts und haelt Massenversuche fern.
 *   <li><b>Passwortvergleich</b> vor jeder Kontopruefung. Wuerde zuerst „gesperrt?" oder „aktiv?"
 *       geprueft, verriete die Antwort einem Fremden, dass es das Konto gibt. Genau deshalb ist
 *       hier kein {@code DaoAuthenticationProvider} im Spiel: der prueft den Kontozustand vor dem
 *       Passwort.
 *   <li><b>Erst danach</b> Sperre und Deaktivierung — und nur dann duerfen sie auch benannt werden.
 *       Wer das richtige Passwort kennt, erfaehrt damit nichts Neues.
 * </ol>
 *
 * <p><b>Auch der unbekannte Benutzername kostet einen BCrypt-Durchlauf.</b> Ohne diesen Vergleich
 * gegen einen Dummy-Hash antwortete der unbekannte Fall in zwei statt zweihundert Millisekunden —
 * Benutzernamen liessen sich dann ueber die Laufzeit durchprobieren, und die gleichlautende
 * Fehlermeldung waere wertlos. Dasselbe gilt fuer ein Konto ohne Passwort-Hash.
 *
 * <p>Diese Klasse fasst die Sitzung <b>nicht</b> an. Sie entscheidet, ob angemeldet wird und
 * welcher Mandant sich daraus ergibt; das Setzen uebernimmt {@link SitzungsVerwaltung} — erst
 * nachdem die Sitzungs-ID erneuert wurde.
 *
 * <p>Alle Zeiten kommen aus der <b>Systemuhr in UTC</b>. Die Anwendungsuhr aus Schritt 2 ist im
 * Dev-Profil um Wochen zurueckversetzt; Sperrfristen liefen damit erst in drei Wochen ab.
 */
@Service
public class AnmeldeService {

  /** Fehlversuche bis zur Sperre. Der fuenfte Fehlversuch setzt {@code locked_until}. */
  static final int MAX_FEHLVERSUCHE = 5;

  /** Dauer der Kontosperre ab dem Fehlversuch, der sie ausgeloest hat. */
  static final Duration SPERRDAUER = Duration.ofMinutes(15);

  private final AppUserRepository appUserRepository;
  private final MandantRepository mandantRepository;
  private final AnmeldeSperre anmeldeSperre;
  private final PasswordEncoder passwortKodierer;
  private final AuditLogWriter auditLogWriter;
  private final Clock systemClock;

  /**
   * Der Hash fuer den Leerlauf-Vergleich. Beim Start aus einem Zufallswert erzeugt, dessen Klartext
   * niemand kennt und der sofort verworfen wird: Ein im Quelltext eingetragener Hash saehe aus wie
   * eine Zugangsdatei und muesste erklaert werden, ein zufaelliger kann nie zu einem Konto passen.
   * Er traegt denselben Kostenfaktor wie jeder echte Hash — sonst waere der Vergleich schneller und
   * der Zweck verfehlt.
   */
  private final String dummyHash;

  AnmeldeService(
      AppUserRepository appUserRepository,
      MandantRepository mandantRepository,
      AnmeldeSperre anmeldeSperre,
      PasswordEncoder passwortKodierer,
      AuditLogWriter auditLogWriter,
      @Qualifier("systemClock") Clock systemClock) {
    this.appUserRepository = appUserRepository;
    this.mandantRepository = mandantRepository;
    this.anmeldeSperre = anmeldeSperre;
    this.passwortKodierer = passwortKodierer;
    this.auditLogWriter = auditLogWriter;
    this.systemClock = systemClock;
    this.dummyHash = passwortKodierer.encode(UUID.randomUUID().toString());
  }

  /**
   * Das Ergebnis einer erfolgreichen Anmeldung.
   *
   * @param aktiverMandant die MandantID, wenn genau eine zulaessig ist — sonst {@code null}
   */
  public record Ergebnis(AngemeldeterNutzer nutzer, String aktiverMandant) {}

  /** Meldet an oder wirft {@link AnmeldungAbgelehntException}. */
  public Ergebnis anmelden(String username, String passwort, String ip) {
    anmeldeSperre.pruefe(ip);

    LocalDateTime jetztUtc = jetztUtc();
    Optional<AppUserZeile> gefunden = appUserRepository.findeNachBenutzername(username);
    String hash = gefunden.map(AppUserZeile::passwortHash).orElse(null);

    // Der BCrypt-Vergleich laeuft IMMER — auch ohne Konto und ohne Hash. Sein Ergebnis zaehlt
    // nur, wenn es einen echten Hash gab.
    boolean vergleichErfolgreich =
        passwortKodierer.matches(passwort, hash != null ? hash : dummyHash);
    boolean passwortStimmt = hash != null && vergleichErfolgreich;

    if (gefunden.isEmpty() || !passwortStimmt) {
      behandleFehlversuch(gefunden.orElse(null), username, ip, jetztUtc);
      throw AnmeldungAbgelehntException.unspezifisch(
          gefunden.isEmpty()
              ? "Unbekannter Benutzername"
              : hash == null ? "Konto ohne Passwort" : "Falsches Passwort");
    }

    AppUserZeile nutzer = gefunden.get();

    // Ab hier ist das Passwort korrekt. Nur deshalb darf der Grund benannt werden.
    if (nutzer.istGesperrt(jetztUtc)) {
      protokolliereFehlversuch(nutzer.id(), username, ip, "Konto gesperrt");
      throw AnmeldungAbgelehntException.gesperrt(nutzer.gesperrtBisUtc());
    }
    if (!nutzer.aktiv()) {
      protokolliereFehlversuch(nutzer.id(), username, ip, "Konto deaktiviert");
      throw AnmeldungAbgelehntException.deaktiviert();
    }

    appUserRepository.merkeAnmeldung(nutzer.id(), jetztUtc);
    anmeldeSperre.zuruecksetzen(ip);
    String aktiverMandant = eindeutigerMandant(nutzer);
    auditLogWriter.schreibe(
        new AuditEvent(
            AuditEventType.ANMELDUNG_ERFOLG,
            nutzer.id(),
            username,
            aktiverMandant,
            null,
            null,
            ip,
            null));
    return new Ergebnis(nutzer.alsAngemeldeterNutzer(), aktiverMandant);
  }

  /**
   * Zaehlt den Fehlversuch und sperrt beim Erreichen der Grenze.
   *
   * <p>Eine <b>abgelaufene</b> Sperre setzt den Zaehler zurueck. Sonst genuegte nach Ablauf ein
   * einziger Fehlversuch, um sofort wieder fuenfzehn Minuten zu sperren — die Frist waere praktisch
   * unbegrenzt.
   */
  private void behandleFehlversuch(
      AppUserZeile nutzer, String username, String ip, LocalDateTime jetztUtc) {
    anmeldeSperre.zaehleFehlversuch(ip);
    if (nutzer == null) {
      // actor_user_id bleibt null, actor_username wird gesetzt — genau das ist der
      // interessanteste Eintrag der Tabelle.
      protokolliereFehlversuch(null, username, ip, "Unbekannter Benutzername");
      return;
    }

    boolean sperreAbgelaufen = nutzer.gesperrtBisUtc() != null && !nutzer.istGesperrt(jetztUtc);
    int bisher = sperreAbgelaufen ? 0 : nutzer.fehlversuche();
    int neu = bisher + 1;
    boolean sperren = neu >= MAX_FEHLVERSUCHE;
    LocalDateTime gesperrtBis = sperren ? jetztUtc.plus(SPERRDAUER) : null;
    appUserRepository.setzeFehlversuche(nutzer.id(), neu, gesperrtBis, jetztUtc);

    protokolliereFehlversuch(nutzer.id(), username, ip, "Falsches Passwort (Versuch " + neu + ")");
    if (sperren) {
      auditLogWriter.schreibe(
          new AuditEvent(
              AuditEventType.KONTO_GESPERRT,
              nutzer.id(),
              username,
              null,
              "app_user",
              String.valueOf(nutzer.id()),
              ip,
              "Gesperrt bis " + gesperrtBis + " UTC nach " + neu + " Fehlversuchen"));
    }
  }

  /**
   * Hat der Nutzer genau einen zulaessigen Mandanten, ist dieser der aktive. Bei mehreren — und bei
   * jedem ADMIN — bleibt er offen; {@code /api/auth/me} meldet dann {@code mandant: null} und jeder
   * fachliche Endpunkt lehnt ab, bis gewaehlt wurde.
   */
  private String eindeutigerMandant(AppUserZeile nutzer) {
    if (nutzer.rolle() == Rolle.ADMIN) {
      // ADMIN ist fuer alle berechtigt. Einen davon zu raten waere die schlechteste Variante:
      // der Admin saehe Daten, die er nicht ausgewaehlt hat.
      return null;
    }
    var zulaessige = mandantRepository.findeFuerNutzer(nutzer.id());
    return zulaessige.size() == 1 ? zulaessige.getFirst().id() : null;
  }

  private void protokolliereFehlversuch(Long userId, String username, String ip, String grund) {
    auditLogWriter.schreibe(
        new AuditEvent(
            AuditEventType.ANMELDUNG_FEHLVERSUCH, userId, username, null, null, null, ip, grund));
  }

  private LocalDateTime jetztUtc() {
    return LocalDateTime.ofInstant(systemClock.instant(), ZoneOffset.UTC);
  }
}
