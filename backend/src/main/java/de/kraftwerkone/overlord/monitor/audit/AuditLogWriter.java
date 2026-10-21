package de.kraftwerkone.overlord.monitor.audit;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.AUDIT_LOG;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schreibt Protokolleintraege nach {@code overlord_monitor.audit_log}.
 *
 * <p>Bewusst in einer <b>eigenen</b> Transaktion ({@link Propagation#REQUIRES_NEW}): Rollt die
 * Fachtransaktion zurueck, muss der Protokolleintrag bestehen bleiben. Ein Fehlversuch, der einen
 * Rollback ausloest, waere sonst genau der Eintrag, der verschwindet.
 *
 * <p>Der Zeitpunkt kommt aus der <b>Systemuhr in UTC</b> ({@code systemClock}), nicht aus der
 * Anwendungsuhr — im eigenen Schema wird UTC gespeichert, und Protokollzeit darf keinen Dev-Versatz
 * tragen.
 */
@Component
public class AuditLogWriter {

  private final DSLContext monitorDsl;
  private final Clock systemClock;

  public AuditLogWriter(
      @Qualifier("monitorDsl") DSLContext monitorDsl, @Qualifier("systemClock") Clock systemClock) {
    this.monitorDsl = monitorDsl;
    this.systemClock = systemClock;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void schreibe(AuditEvent ereignis) {
    fuegeEin(ereignis);
  }

  /**
   * Schreibt den Eintrag in die <b>laufende</b> Transaktion des Aufrufers ({@link
   * Propagation#MANDATORY}) — das Gegenstueck zu {@link #schreibe}, fuer den einen Fall, in dem der
   * Eintrag mit der Fachaenderung stehen oder fallen muss.
   *
   * <p>Gebraucht seit A4 beim Loeschen eines Auftrags ({@code docs/tasks.md} §6.6, F26): Rollt das
   * Loeschen zurueck, darf kein Eintrag „geloescht“ bleiben, und scheitert der Eintrag, darf die
   * Zeile nicht verschwunden sein. {@code MANDATORY} wirft, wenn keine Transaktion laeuft, statt
   * still eine eigene zu oeffnen.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void schreibeInTransaktion(AuditEvent ereignis) {
    fuegeEin(ereignis);
  }

  private void fuegeEin(AuditEvent ereignis) {
    LocalDateTime jetztUtc = LocalDateTime.ofInstant(systemClock.instant(), ZoneOffset.UTC);
    monitorDsl
        .insertInto(AUDIT_LOG)
        .set(AUDIT_LOG.OCCURRED_AT, jetztUtc)
        .set(AUDIT_LOG.EVENT_TYPE, ereignis.typ().name())
        .set(AUDIT_LOG.ACTOR_USER_ID, ereignis.actorUserId())
        .set(AUDIT_LOG.ACTOR_USERNAME, ereignis.actorUsername())
        .set(AUDIT_LOG.MANDANT_ID, ereignis.mandantId())
        .set(AUDIT_LOG.TARGET_TYPE, ereignis.targetType())
        .set(AUDIT_LOG.TARGET_ID, ereignis.targetId())
        .set(AUDIT_LOG.IP, ereignis.ip())
        .set(AUDIT_LOG.DETAIL, ereignis.detail())
        .execute();
  }
}
