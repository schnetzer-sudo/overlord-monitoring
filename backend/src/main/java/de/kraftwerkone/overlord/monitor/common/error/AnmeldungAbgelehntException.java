package de.kraftwerkone.overlord.monitor.common.error;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

/**
 * Die Anmeldung wurde abgelehnt. {@code 401}.
 *
 * <p><b>Auskunftsdisziplin.</b> Der Regelfall ist die unspezifische Meldung: Sie unterscheidet
 * nicht zwischen „Benutzer unbekannt" und „Passwort falsch". Wer beides unterscheiden kann, kann
 * Benutzernamen durchprobieren.
 *
 * <p><b>Genau eine Ausnahme davon</b>: War das Passwort korrekt und das Konto ist gesperrt —
 * automatisch oder durch einen Admin — oder deaktiviert, darf das benannt werden. Wer das Passwort
 * kennt, erfaehrt damit nichts Neues — wer es nicht kennt, bekommt weiterhin {@link
 * #unspezifisch()}. Ohne diese Ausnahme rennt ein berechtigter Nutzer fuenfzehn Minuten gegen eine
 * Wand, ohne den Grund zu erfahren.
 */
public class AnmeldungAbgelehntException extends FachlicheAusnahme {

  private AnmeldungAbgelehntException(
      String problemTyp, String titel, String detail, String interneUrsache) {
    super(HttpStatus.UNAUTHORIZED, problemTyp, titel, detail, interneUrsache);
  }

  /** Unbekannter Benutzername oder falsches Passwort — nach aussen ununterscheidbar. */
  public static AnmeldungAbgelehntException unspezifisch(String interneUrsache) {
    return new AnmeldungAbgelehntException(
        "anmeldung-abgelehnt",
        "Anmeldung fehlgeschlagen",
        "Benutzername oder Passwort ist falsch.",
        interneUrsache);
  }

  /** Passwort korrekt, Konto gesperrt. Der Zeitpunkt darf genannt werden. */
  public static AnmeldungAbgelehntException gesperrt(LocalDateTime bisUtc) {
    return new AnmeldungAbgelehntException(
        "konto-gesperrt",
        "Konto gesperrt",
        "Das Konto ist nach mehreren Fehlversuchen gesperrt. Versuche es spaeter erneut oder wende"
            + " dich an die EDI-Betreuung.",
        "Anmeldung mit korrektem Passwort auf gesperrtes Konto, gesperrt bis " + bisUtc + " UTC");
  }

  /**
   * Passwort korrekt, Konto <b>durch einen Admin</b> gesperrt (Schritt 9a).
   *
   * <p><b>Ein eigener Problemtyp und ein eigener Text, nicht der von {@link #gesperrt}.</b> Der
   * sagt woertlich „nach mehreren Fehlversuchen" — bei einem Verwaltungsakt waere das eine falsche
   * Auskunft, und der Nutzer wartete fuenfzehn Minuten auf einen Ablauf, der nie kommt. Es ist
   * dieselbe Trennung, die E14 fuer die Ereignisarten verlangt, hier auf der Auskunft: Ein Angriff
   * und ein Verwaltungsakt sind nicht dasselbe, auch nicht fuer den, den es trifft.
   *
   * <p>Die Auskunftsdisziplin bleibt unberuehrt: Der Fall wird erst erreicht, wenn das Passwort
   * stimmt.
   */
  public static AnmeldungAbgelehntException durchAdminGesperrt() {
    return new AnmeldungAbgelehntException(
        "konto-administrativ-gesperrt",
        "Konto gesperrt",
        "Das Konto wurde von der EDI-Betreuung gesperrt. Wende dich an sie.",
        "Anmeldung mit korrektem Passwort auf administrativ gesperrtes Konto");
  }

  /** Passwort korrekt, Konto deaktiviert. */
  public static AnmeldungAbgelehntException deaktiviert() {
    return new AnmeldungAbgelehntException(
        "konto-deaktiviert",
        "Konto deaktiviert",
        "Das Konto ist deaktiviert. Wende dich an die EDI-Betreuung.",
        "Anmeldung mit korrektem Passwort auf deaktiviertes Konto");
  }
}
