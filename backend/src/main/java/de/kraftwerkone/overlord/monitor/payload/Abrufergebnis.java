package de.kraftwerkone.overlord.monitor.payload;

/**
 * Was ein Abruf bei der Ablage ergeben hat: entweder die gepackten Bytes oder ein benannter
 * Zustand.
 *
 * <p><b>Es gibt keinen dritten Fall und keine Ausnahme nach aussen.</b> Der Zugriff auf eine fremde
 * Maschine schlaegt fehl — das ist ein Betriebszustand und kein Programmfehler. Wuerde er als
 * Ausnahme geworfen, landete er im {@code GlobalExceptionHandler} als {@code 500}, und der
 * Unterschied zwischen „Datei weg" und „Ablage aus" ginge dabei verloren. Genau diesen Unterschied
 * braucht der Betrieb.
 *
 * @param zustand {@link Artefaktzustand#ANZEIGBAR} nur zusammen mit {@link #zip()}
 * @param zip die Bytes der ZIP-Datei; {@code null} in jedem anderen Zustand
 */
public record Abrufergebnis(Artefaktzustand zustand, byte[] zip) {

  /** Die Ablage hat geantwortet und einen Anhang geliefert. */
  public static Abrufergebnis geholt(byte[] zip) {
    return new Abrufergebnis(Artefaktzustand.ANZEIGBAR, zip);
  }

  /**
   * Die Ablage hat geantwortet, aber nichts geliefert. Produktiv ein Fehlerzustand; lokal der
   * Normalfall fuer 63,2 % des Bestands.
   */
  public static Abrufergebnis nichtVorhanden() {
    return new Abrufergebnis(Artefaktzustand.DATEI_NICHT_VORHANDEN, null);
  }

  /** Die Kennung loest nicht auf, oder der Knoten hat nicht geantwortet. */
  public static Abrufergebnis nichtErreichbar() {
    return new Abrufergebnis(Artefaktzustand.ABLAGE_NICHT_ERREICHBAR, null);
  }

  public boolean erfolgreich() {
    return zustand == Artefaktzustand.ANZEIGBAR && zip != null;
  }

  /**
   * Bewusst ueberschrieben: kein Byte des Anhangs geraet ueber eine Protokollzeile nach draussen.
   */
  @Override
  public String toString() {
    return "Abrufergebnis["
        + zustand
        + ", "
        + (zip == null ? "ohne Anhang" : zip.length + " Byte]");
  }
}
