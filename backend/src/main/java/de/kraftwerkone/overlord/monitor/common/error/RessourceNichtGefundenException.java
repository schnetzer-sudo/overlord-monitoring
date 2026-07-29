package de.kraftwerkone.overlord.monitor.common.error;

import org.springframework.http.HttpStatus;

/**
 * Eine angeforderte Ressource existiert nicht — <b>oder</b> sie gehoert einem fremden Mandanten.
 * Beide Faelle sind fuer den Nutzer ununterscheidbar und liefern {@code 404}, niemals {@code 403}:
 * ein {@code 403} verriete, dass der Datensatz existiert. Fuer den Nutzer hat die Zeile nie
 * existiert (Regel M3).
 *
 * <p>Deshalb ist der Antworttext hier <b>fest</b>. Die uebergebene Ursache geht ausschliesslich ins
 * Protokoll: stuende sie in der Antwort, unterschiede sich „gibt es nicht" wieder von „gehoert
 * jemand anderem".
 */
public class RessourceNichtGefundenException extends FachlicheAusnahme {

  public RessourceNichtGefundenException(String interneUrsache) {
    super(
        HttpStatus.NOT_FOUND,
        "nicht-gefunden",
        "Nicht gefunden",
        "Die angeforderte Ressource wurde nicht gefunden.",
        interneUrsache);
  }
}
