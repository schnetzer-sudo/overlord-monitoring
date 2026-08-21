package de.kraftwerkone.overlord.monitor.security;

import java.time.LocalDateTime;

/**
 * Eine Zeile aus {@code overlord_monitor.app_user}, wie sie die Anmeldung braucht.
 *
 * <p><b>Traegt den Passwort-Hash und verlaesst deshalb niemals dieses Paket.</b> Nach aussen geht
 * ausschliesslich {@link AngemeldeterNutzer}. Dieser Satz ist der Grund, weshalb der Typ nicht als
 * Antwort-DTO taugt und auch nicht dazu ausgebaut wird.
 *
 * <p>Alle Zeitpunkte sind UTC — im eigenen Schema wird UTC gespeichert, und sicherheitsrelevante
 * Zeit rechnet mit der Systemuhr, nie mit der Anwendungsuhr aus Schritt 2.
 *
 * <p><b>Zwei Sperren, zwei Felder — bewusst.</b> {@link #gesperrtBisUtc} ist die automatische
 * Sperre nach fuenf Fehlversuchen und laeuft von selbst ab; {@link #adminGesperrt} ist der
 * Verwaltungsakt aus Schritt 9a und laeuft nie ab. Sie in eine Spalte zu legen waere nicht nur
 * unsauber, sondern defekt: Ein einzelner Fehlversuch schreibt {@code locked_until} bedingungslos
 * neu und loeschte damit die administrative Sperre. Begruendung in {@code
 * V7__benutzerverwaltung.sql}.
 *
 * @param passwortHash {@code null} bei einem Konto, das noch nie ein Passwort bekommen hat. Ein
 *     solches Konto kann sich nie anmelden, durchlaeuft aber denselben Dummy-Vergleich.
 */
public record AppUserZeile(
    long id,
    String username,
    String passwortHash,
    Rolle rolle,
    boolean aktiv,
    boolean passwortwechselErforderlich,
    boolean adminGesperrt,
    int fehlversuche,
    LocalDateTime gesperrtBisUtc) {

  /** Ob die <b>automatische</b> Sperre zum angegebenen Zeitpunkt (Systemuhr, UTC) noch laeuft. */
  public boolean istGesperrt(LocalDateTime jetztUtc) {
    return gesperrtBisUtc != null && gesperrtBisUtc.isAfter(jetztUtc);
  }

  public AngemeldeterNutzer alsAngemeldeterNutzer() {
    return new AngemeldeterNutzer(id, username, rolle, passwortwechselErforderlich);
  }
}
