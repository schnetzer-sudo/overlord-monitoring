package de.kraftwerkone.overlord.monitor.common.error;

import org.springframework.http.HttpStatus;

/**
 * Ein <b>vorhergesehener</b> Fehler: zu grosses Zeitfenster, gesperrtes Konto, fremder Mandant. Er
 * traegt Status, Problemtyp, Titel und einen deutschen, fuer den Nutzer lesbaren Text — und wird
 * vom {@link GlobalExceptionHandler} unveraendert in eine {@code problem+json}-Antwort uebersetzt.
 *
 * <p>Fachliche Fehler werden ohne Stacktrace protokolliert. Technische Fehler sind alles andere;
 * sie ergeben {@code 500} und einen neutralen Text.
 *
 * <p><b>Zwei Texte, bewusst getrennt.</b> {@link #detail()} geht an den Nutzer und enthaelt niemals
 * Stacktraces, SQL, Tabellen-, Spalten- oder Klassennamen, Hostnamen — und niemals ein Passwort.
 * {@link #getMessage()} ist die interne Ursache fuers Protokoll und darf konkreter sein. Wo beide
 * gleich sind, genuegt der kurze Konstruktor.
 */
public class FachlicheAusnahme extends RuntimeException {

  private final HttpStatus status;
  private final String problemTyp;
  private final String titel;
  private final String detail;

  /**
   * @param status der HTTP-Status der Antwort
   * @param problemTyp letzter Pfadabschnitt der Problemtyp-URI, kebab-case (z. B. {@code
   *     konto-gesperrt})
   * @param titel kurze deutsche Ueberschrift
   * @param detail deutscher Text mit Handlungshinweis; wird dem Nutzer unveraendert gezeigt
   */
  public FachlicheAusnahme(HttpStatus status, String problemTyp, String titel, String detail) {
    this(status, problemTyp, titel, detail, detail);
  }

  /**
   * @param interneUrsache Text fuers Protokoll. Erscheint <b>nicht</b> in der Antwort und darf
   *     deshalb konkreter sein als {@code detail} — aber auch hier nie ein Passwort.
   */
  public FachlicheAusnahme(
      HttpStatus status, String problemTyp, String titel, String detail, String interneUrsache) {
    super(interneUrsache);
    this.status = status;
    this.problemTyp = problemTyp;
    this.titel = titel;
    this.detail = detail;
  }

  public HttpStatus status() {
    return status;
  }

  public String problemTyp() {
    return problemTyp;
  }

  public String titel() {
    return titel;
  }

  /** Der dem Nutzer gezeigte Text. */
  public String detail() {
    return detail;
  }
}
