package de.kraftwerkone.overlord.monitor.common.error;

/**
 * Eine angeforderte Ressource existiert nicht — <b>oder</b> sie gehoert einem fremden Mandanten.
 * Beide Faelle sind fuer den Nutzer ununterscheidbar und liefern {@code 404}, niemals {@code 403}:
 * ein {@code 403} verriete, dass der Datensatz existiert. Fuer den Nutzer hat die Zeile nie
 * existiert (Regel M3).
 */
public class RessourceNichtGefundenException extends RuntimeException {

  public RessourceNichtGefundenException(String message) {
    super(message);
  }
}
