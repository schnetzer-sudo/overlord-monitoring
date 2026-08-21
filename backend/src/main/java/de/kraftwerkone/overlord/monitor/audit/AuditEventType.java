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
  /** PAYLOAD_VIEWED — Artefakt im Browser angesehen (Schritt 8). */
  ROHDATEN_ANGESEHEN,
  /** PAYLOAD_DOWNLOADED — Artefakt heruntergeladen (Schritt 8). */
  ROHDATEN_DOWNLOAD,
  /** PAYLOAD_FAILED — Abruf fehlgeschlagen, mit dem Zustand im Detail (Schritt 8). */
  ROHDATEN_ABRUF_FEHLGESCHLAGEN,

  // ─── Benutzerverwaltung, Schritt 9a ────────────────────────────────────────
  //
  // Ein Endpunkt, ein Vorgang, eine Ereignisart. Ein gemeinsames PATCH, das
  // Rolle, Mandanten und Sperrzustand in einem Aufruf aendern koennte, erzeugte
  // eine Zeile, die entweder aufgespalten werden muss oder zu einem
  // nichtssagenden NUTZER_GEAENDERT verwaessert — und dann ist das Protokoll bei
  // einem Vorfall nicht mehr lesbar (docs/benutzerverwaltung.md §5).
  //
  // Der Sitzungsentzug bekommt KEINE eigene Art (E15). Er ist nie ein
  // eigenstaendiger Vorgang und stuende sonst als Anhaengsel in jeder zweiten
  // Zeile; die Zahl verworfener Sitzungen steht im detail des ausloesenden
  // Ereignisses.
  //
  // In keiner dieser Zeilen steht jemals ein Passwort — auch nicht gehasht,
  // auch nicht abgekuerzt.

  /**
   * USER_LOCKED_BY_ADMIN — die <b>administrative</b> Sperre (Schritt 9a).
   *
   * <p><b>Nicht {@link #KONTO_GESPERRT}.</b> Das steht seit Schritt 2 fuer die automatische Sperre
   * nach fuenf Fehlversuchen. Neben ihr waere {@code NUTZER_GESPERRT} um drei Uhr nachts nicht
   * auseinanderzuhalten — deshalb benennt der Name die <b>Ursache</b> statt des Objekts, und die
   * Ursache ist der Unterschied: ein Angriff und ein Verwaltungsakt gehoeren nicht in dieselbe
   * Zeile (E14).
   */
  SPERRE_DURCH_ADMIN,
  /**
   * USER_UNLOCKED_BY_ADMIN — hebt sowohl die administrative als auch eine laufende Zeitsperre auf.
   */
  ENTSPERRT_DURCH_ADMIN,
  /** USER_DEACTIVATED — es gibt kein Loeschen (E8). */
  NUTZER_DEAKTIVIERT,
  /** USER_REACTIVATED */
  NUTZER_REAKTIVIERT,
  /** USER_ROLE_CHANGED — mit alter und neuer Rolle im Detail. */
  ROLLE_GEAENDERT,
  /** USER_TENANTS_CHANGED — mit alter und neuer Menge im Detail (dritte M1-Ausnahme). */
  MANDANTEN_GEAENDERT,
  /** USER_PASSWORD_RESET — durch den Admin. <b>Niemals mit dem Passwort im Detail.</b> */
  PASSWORT_ZURUECKGESETZT
}
