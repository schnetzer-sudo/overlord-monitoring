package de.kraftwerkone.overlord.monitor.audit;

/**
 * Whitelist der protokollierten Ereignisarten. Bewusst ein Aufzaehlungstyp im Code statt ein {@code
 * ENUM} in der Datenbank: ein neuer Ereignistyp soll keine Migration kosten. In {@code
 * audit_log.event_type} wird {@link #name()} gespeichert.
 *
 * <p>Die konkreten Ausloeser entstehen in spaeteren Schritten (Anmeldung ab Schritt 3, Download ab
 * Schritt 8, Katalog ab Schritt 9); die Arten stehen hier von Anfang an fest.
 *
 * <p><b>Die Namen bleiben deutsch.</b> Fuenf Arten sind seit Schritt 2 vergeben und stehen bereits
 * als Text in {@code audit_log.event_type}; eine Umbenennung machte vorhandene Zeilen unlesbar,
 * ohne irgendetwas zu gewinnen. Die Zuordnung zu den englischen Bezeichnungen aus der
 * Aufgabenstellung steht in {@code docs/authentifizierung.md} und als Kommentar an jeder Zeile.
 */
public enum AuditEventType {
  /** LOGIN_SUCCESS */
  ANMELDUNG_ERFOLG,
  /** LOGIN_FAILED — mit {@code actor_user_id = NULL} bei unbekanntem Benutzernamen. */
  ANMELDUNG_FEHLVERSUCH,
  /** LOGOUT */
  ABMELDUNG,
  /** ACCOUNT_LOCKED */
  KONTO_GESPERRT,
  /** PASSWORD_CHANGED */
  PASSWORT_GEAENDERT,
  /** MANDANT_SWITCHED — mit altem und neuem Mandanten (Schritt 3). */
  MANDANT_GEWECHSELT,
  /** USER_BOOTSTRAPPED — das erste Admin-Konto im Profil {@code bootstrap} (Schritt 3). */
  NUTZER_BOOTSTRAP,
  /** USER_CREATED — Anlegen ueber {@code POST /api/admin/users} (Schritt 3). */
  NUTZER_ANGELEGT,
  /** CATALOG_CHANGED */
  KATALOG_GEAENDERT,
  /** PAYLOAD_DOWNLOADED */
  ROHDATEN_DOWNLOAD
}
