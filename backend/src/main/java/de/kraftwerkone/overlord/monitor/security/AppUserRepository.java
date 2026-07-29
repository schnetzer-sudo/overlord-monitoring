package de.kraftwerkone.overlord.monitor.security;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.APP_USER;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.APP_USER_MANDANT;

import java.time.LocalDateTime;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Zugriff auf {@code overlord_monitor.app_user} und {@code app_user_mandant} — das <b>eigene</b>
 * Schema, deshalb ueber {@code monitorDsl} und deshalb schreibend.
 *
 * <p>Hier gilt Regel M2 <b>nicht</b>: Diese Klasse fasst {@code jooq.glassfish} nicht an. Ein
 * {@link MandantContext} waere hier auch inhaltlich falsch — ein Konto ist keinem Mandanten
 * unterworfen, es <i>traegt</i> seine Mandanten.
 *
 * <p>Die Zeitpunkte kommen von aussen und sind immer UTC aus der <b>Systemuhr</b>. Sperrfristen
 * duerfen niemals mit der Anwendungsuhr rechnen, die im Dev-Profil um Wochen zurueckversetzt ist.
 */
@Repository
public class AppUserRepository {

  private final DSLContext monitorDsl;

  AppUserRepository(@Qualifier("monitorDsl") DSLContext monitorDsl) {
    this.monitorDsl = monitorDsl;
  }

  public Optional<AppUserZeile> findeNachBenutzername(String username) {
    return monitorDsl
        .select(
            APP_USER.ID,
            APP_USER.USERNAME,
            APP_USER.PASSWORD_HASH,
            APP_USER.ROLE,
            APP_USER.ENABLED,
            APP_USER.MUST_CHANGE_PASSWORD,
            APP_USER.DOWNLOAD_ALLOWED,
            APP_USER.FAILED_ATTEMPTS,
            APP_USER.LOCKED_UNTIL)
        .from(APP_USER)
        .where(APP_USER.USERNAME.eq(username))
        .fetchOptional()
        .map(AppUserRepository::zuZeile);
  }

  public Optional<AppUserZeile> findeNachId(long id) {
    return monitorDsl
        .select(
            APP_USER.ID,
            APP_USER.USERNAME,
            APP_USER.PASSWORD_HASH,
            APP_USER.ROLE,
            APP_USER.ENABLED,
            APP_USER.MUST_CHANGE_PASSWORD,
            APP_USER.DOWNLOAD_ALLOWED,
            APP_USER.FAILED_ATTEMPTS,
            APP_USER.LOCKED_UNTIL)
        .from(APP_USER)
        .where(APP_USER.ID.eq(id))
        .fetchOptional()
        .map(AppUserRepository::zuZeile);
  }

  public boolean existiertBenutzername(String username) {
    return monitorDsl.fetchExists(
        monitorDsl.selectOne().from(APP_USER).where(APP_USER.USERNAME.eq(username)));
  }

  /** Ob ueberhaupt schon ein Konto mit Rolle ADMIN existiert — die Bedingung des Bootstraps. */
  public boolean existiertAdmin() {
    return monitorDsl.fetchExists(
        monitorDsl.selectOne().from(APP_USER).where(APP_USER.ROLE.eq(Rolle.ADMIN.name())));
  }

  /**
   * Schreibt Fehlversuchszaehler und Sperrfrist fort.
   *
   * @param gesperrtBisUtc {@code null} loescht eine bestehende Sperre
   */
  public void setzeFehlversuche(
      long id, int anzahl, LocalDateTime gesperrtBisUtc, LocalDateTime jetztUtc) {
    monitorDsl
        .update(APP_USER)
        .set(APP_USER.FAILED_ATTEMPTS, anzahl)
        .set(APP_USER.LOCKED_UNTIL, gesperrtBisUtc)
        .set(APP_USER.UPDATED_AT, jetztUtc)
        .where(APP_USER.ID.eq(id))
        .execute();
  }

  /** Erfolgreiche Anmeldung: Zaehler und Sperre zuruecksetzen, Zeitpunkt merken. */
  public void merkeAnmeldung(long id, LocalDateTime jetztUtc) {
    monitorDsl
        .update(APP_USER)
        .set(APP_USER.FAILED_ATTEMPTS, 0)
        .set(APP_USER.LOCKED_UNTIL, (LocalDateTime) null)
        .set(APP_USER.LAST_LOGIN_AT, jetztUtc)
        .set(APP_USER.UPDATED_AT, jetztUtc)
        .where(APP_USER.ID.eq(id))
        .execute();
  }

  /** Neues Passwort setzen und den Aenderungszwang aufheben. */
  public void setzePasswort(long id, String hash, LocalDateTime jetztUtc) {
    monitorDsl
        .update(APP_USER)
        .set(APP_USER.PASSWORD_HASH, hash)
        .set(APP_USER.MUST_CHANGE_PASSWORD, false)
        .set(APP_USER.UPDATED_AT, jetztUtc)
        .where(APP_USER.ID.eq(id))
        .execute();
  }

  /**
   * Legt ein Konto an und liefert dessen ID. Aufrufer pruefen den Benutzernamen vorher; der
   * eindeutige Index ist die Wahrheit und schlaegt bei einem Wettlauf trotzdem zu.
   */
  public long legeAn(
      String username,
      String passwortHash,
      Rolle rolle,
      boolean aktiv,
      boolean passwortwechselErforderlich,
      LocalDateTime jetztUtc) {
    return monitorDsl
        .insertInto(APP_USER)
        .set(APP_USER.USERNAME, username)
        .set(APP_USER.PASSWORD_HASH, passwortHash)
        .set(APP_USER.ROLE, rolle.name())
        .set(APP_USER.ENABLED, aktiv)
        .set(APP_USER.MUST_CHANGE_PASSWORD, passwortwechselErforderlich)
        .set(APP_USER.CREATED_AT, jetztUtc)
        .set(APP_USER.UPDATED_AT, jetztUtc)
        .returningResult(APP_USER.ID)
        .fetchOne(APP_USER.ID);
  }

  /**
   * Ordnet einem Konto einen Mandanten zu. Kein Fremdschluessel auf {@code GlassfishDB.Mandant}.
   */
  public void ordneMandantZu(long userId, String mandantId) {
    monitorDsl
        .insertInto(APP_USER_MANDANT)
        .set(APP_USER_MANDANT.USER_ID, userId)
        .set(APP_USER_MANDANT.MANDANT_ID, mandantId)
        .execute();
  }

  private static AppUserZeile zuZeile(Record satz) {
    return new AppUserZeile(
        satz.get(APP_USER.ID),
        satz.get(APP_USER.USERNAME),
        satz.get(APP_USER.PASSWORD_HASH),
        Rolle.ausDatenbank(satz.get(APP_USER.ROLE)),
        Boolean.TRUE.equals(satz.get(APP_USER.ENABLED)),
        Boolean.TRUE.equals(satz.get(APP_USER.MUST_CHANGE_PASSWORD)),
        Boolean.TRUE.equals(satz.get(APP_USER.DOWNLOAD_ALLOWED)),
        satz.get(APP_USER.FAILED_ATTEMPTS),
        satz.get(APP_USER.LOCKED_UNTIL));
  }
}
