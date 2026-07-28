package de.kraftwerkone.overlord.monitor.audit;

/**
 * Whitelist der protokollierten Ereignisarten. Bewusst ein Aufzaehlungstyp im Code statt ein {@code
 * ENUM} in der Datenbank: ein neuer Ereignistyp soll keine Migration kosten. In {@code
 * audit_log.event_type} wird {@link #name()} gespeichert.
 *
 * <p>Die konkreten Ausloeser entstehen in spaeteren Schritten (Anmeldung ab Schritt 3, Download ab
 * Schritt 8, Katalog ab Schritt 9); die Arten stehen hier von Anfang an fest.
 */
public enum AuditEventType {
  ANMELDUNG_ERFOLG,
  ANMELDUNG_FEHLVERSUCH,
  ABMELDUNG,
  KONTO_GESPERRT,
  PASSWORT_GEAENDERT,
  KATALOG_GEAENDERT,
  ROHDATEN_DOWNLOAD
}
