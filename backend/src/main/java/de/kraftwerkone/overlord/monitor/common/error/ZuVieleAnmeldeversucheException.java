package de.kraftwerkone.overlord.monitor.common.error;

import org.springframework.http.HttpStatus;

/**
 * Zu viele Anmeldeversuche von derselben IP-Adresse. {@code 429}.
 *
 * <p>Die Begrenzung liegt <b>ausschliesslich im Arbeitsspeicher</b> und schreibt niemals in die
 * Datenbank — sonst waere der Schutzmechanismus selbst der Angriffsvektor, weil jeder Bot
 * Schreiblast erzeugte.
 *
 * <p>Die Meldung verraet nichts ueber ein Konto: sie betrifft die Herkunft der Anfrage, nicht den
 * Benutzernamen.
 */
public class ZuVieleAnmeldeversucheException extends FachlicheAusnahme {

  public ZuVieleAnmeldeversucheException(String interneUrsache) {
    super(
        HttpStatus.TOO_MANY_REQUESTS,
        "zu-viele-anmeldeversuche",
        "Zu viele Anmeldeversuche",
        "Von dieser Adresse kamen zu viele Anmeldeversuche. Versuche es in einigen Minuten erneut.",
        interneUrsache);
  }
}
