package de.kraftwerkone.overlord.monitor.audit;

/**
 * Ein zu protokollierendes Ereignis. Der Zeitpunkt wird nicht hier gefuehrt, sondern vom {@link
 * AuditLogWriter} aus der Systemuhr gesetzt.
 *
 * <p>{@code actorUsername} steht zusaetzlich zur {@code actorUserId}, weil ein Fehlversuch auf
 * einen unbekannten Namen keine ID hat — und genau das der interessanteste Eintrag der Tabelle ist.
 * Alle Felder ausser {@code typ} duerfen {@code null} sein.
 *
 * <p><b>Nie befuellen</b> mit Passwoertern, Session-IDs, Cookie-Werten oder Inhalten von
 * EDI-Nutzdaten bzw. {@code MessagePropertyValue}.
 */
public record AuditEvent(
    AuditEventType typ,
    Long actorUserId,
    String actorUsername,
    String mandantId,
    String targetType,
    String targetId,
    String ip,
    String detail) {

  public AuditEvent {
    if (typ == null) {
      throw new IllegalArgumentException("AuditEvent.typ darf nicht null sein");
    }
  }
}
