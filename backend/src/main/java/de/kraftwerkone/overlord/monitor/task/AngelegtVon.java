package de.kraftwerkone.overlord.monitor.task;

import de.kraftwerkone.overlord.monitor.security.Rolle;

/**
 * Wer einen Auftrag angelegt hat, wie die Antwort es zeigt ({@code docs/tasks.md} §2 „Ersteller am
 * Auftrag“, §6.5).
 *
 * <p><b>Die Einordnung steht nur hier.</b> Sie gilt fuer jeden Betrachter gleich (F56) und nach der
 * <b>heutigen</b> Rolle des Kontos (E‑1180): Aendert sich die Rolle, aendert sich die Anzeige alter
 * Auftraege mit. Wer angelegt hat, steht unveraendert in {@code task.angelegt_von} und im {@code
 * audit_log}.
 *
 * <p><b>Bei {@code ADMIN} verlaesst der Benutzername das Backend nicht</b> (E‑1179), auch nicht in
 * einem anderen Feld.
 *
 * @param art wie das Konto einzuordnen ist
 * @param name der Benutzername bei {@link Art#BENUTZER}, der gespeicherte Wert bei {@link
 *     Art#UNBEKANNT}, {@code null} bei {@link Art#ADMIN}
 */
public record AngelegtVon(Art art, String name) {

  /** Vollstaendig; die Oberflaeche zeigt einen Wert, den sie nicht kennt, ohne Namen (Q4). */
  public enum Art {
    BENUTZER,
    ADMIN,
    UNBEKANNT
  }

  /**
   * Ordnet ein. <b>Nicht ueber {@link Rolle#ausDatenbank}:</b> Ein unbekannter Wert in {@code
   * app_user.role} ist dort ein Datenfehler, der hier die ganze Liste des Mandanten mitnaehme; nach
   * F55 zaehlt allein, ob die Rolle {@code ADMIN} ist.
   *
   * @param angelegtVon der Wert aus {@code task.angelegt_von}
   * @param rolle {@code app_user.role} des Kontos; {@code null}, wenn es kein Konto dazu gibt
   */
  static AngelegtVon einordnen(String angelegtVon, String rolle) {
    if (rolle == null) {
      return new AngelegtVon(Art.UNBEKANNT, angelegtVon);
    }
    if (Rolle.ADMIN.name().equals(rolle)) {
      return new AngelegtVon(Art.ADMIN, null);
    }
    return new AngelegtVon(Art.BENUTZER, angelegtVon);
  }
}
