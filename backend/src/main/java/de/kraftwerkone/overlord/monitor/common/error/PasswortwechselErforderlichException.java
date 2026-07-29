package de.kraftwerkone.overlord.monitor.common.error;

import org.springframework.http.HttpStatus;

/**
 * Das Konto steht unter Aenderungszwang. {@code 403}.
 *
 * <p>Solange {@code must_change_password} gesetzt ist, ist der Nutzer zwar angemeldet, aber jeder
 * Endpunkt ausser Selbstauskunft, Passwortaenderung und Abmeldung lehnt ab. Jedes neu angelegte
 * Konto — auch das Bootstrap-Konto — startet in diesem Zustand; die Umgebungsvariable
 * beziehungsweise das vom Admin vergebene Passwort transportiert damit nur ein Einmalpasswort.
 */
public class PasswortwechselErforderlichException extends FachlicheAusnahme {

  public PasswortwechselErforderlichException(String interneUrsache) {
    super(
        HttpStatus.FORBIDDEN,
        "passwortwechsel-erforderlich",
        "Passwortwechsel erforderlich",
        "Vergib zuerst ein neues Passwort. Bis dahin sind die uebrigen Funktionen gesperrt.",
        interneUrsache);
  }
}
