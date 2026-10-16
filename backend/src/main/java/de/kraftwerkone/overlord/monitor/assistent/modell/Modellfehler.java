package de.kraftwerkone.overlord.monitor.assistent.modell;

/**
 * Ein Fehler des Anbieters, abgebildet auf eine eigene Art ({@code docs/assistent.md} §4.3). Die
 * Meldung ist fuer das Protokoll und nennt hoechstens HTTP-Status und Fehlertyp des Anbieters, nie
 * den Text einer Nachricht. Der Client sieht sie nicht.
 */
public class Modellfehler extends RuntimeException {

  /** Die Arten, auf die jeder Adapter seine Fehler abbildet. */
  public enum Art {
    /** Keine Antwort innerhalb der Zeitgrenze. */
    ZEITGRENZE,
    /** Keine Verbindung zum Anbieter. */
    NICHT_ERREICHBAR,
    /** Der Anbieter ist ueberlastet oder begrenzt, auch nach dem einen Wiederholungsversuch. */
    UEBERLASTET,
    /** Der Anbieter lehnt die Anfrage ab ({@code 4xx}), etwa wegen eines falschen Schluessels. */
    ABGELEHNT,
    /** Stoerung beim Anbieter ({@code 5xx}). */
    STOERUNG,
    /** Die Antwort ist nicht lesbar. */
    UNLESBAR;

    /** Ob die Stoerung voruebergehend ist ({@code 503}) oder nicht ({@code 502}). */
    public boolean voruebergehend() {
      return this == ZEITGRENZE || this == NICHT_ERREICHBAR || this == UEBERLASTET;
    }
  }

  private final Art art;

  public Modellfehler(Art art, String meldung) {
    super(meldung);
    this.art = art;
  }

  public Modellfehler(Art art, String meldung, Throwable ursache) {
    super(meldung, ursache);
    this.art = art;
  }

  public Art art() {
    return art;
  }
}
