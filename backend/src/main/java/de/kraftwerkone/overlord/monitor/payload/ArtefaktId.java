package de.kraftwerkone.overlord.monitor.payload;

import java.util.Optional;

/**
 * Die Kennung eines Artefakts <b>innerhalb seiner Nachricht</b>: Schritt und Name, sonst nichts.
 *
 * <h2>Was sie niemals enthaelt</h2>
 *
 * <p><b>Keine GUID und keine Ablagenkennung.</b> Das ist die sicherheitskritische Zeile dieses
 * Features, nicht eine Stilfrage. Naehme ein Endpunkt einen Filestore-Verweis entgegen, waere er
 * ein offener Proxy vor einer Produktionsablage: Wer einen fremden Verweis kennt oder raet, holt
 * sich eine fremde Datei — und die Mandantenpruefung liefe ins Leere, weil sie an der
 * <i>Nachricht</i> haengt und nicht an der Datei. Genau das ist im Altsystem der Fall ({@code
 * JsonServlet.java:165}–{@code :171}, Q5). Hier wird der Verweis <b>serverseitig</b> aus {@code
 * messageId} und dieser Kennung hergeleitet und nie entgegengenommen.
 *
 * <h2>Warum sie trotzdem lesbar ist</h2>
 *
 * <p>Sie ist keine Berechtigung, sondern eine Auswahl unter den Artefakten <i>einer</i> Nachricht.
 * Der Server liest zuerst die Artefakte dieser Nachricht — mandantengefiltert im Statement — und
 * sucht darin die passende Zeile. Eine erfundene Kennung findet nichts; eine Kennung aus einer
 * fremden Nachricht ebenso wenig, weil die Menge, in der gesucht wird, gar nicht erst fremde Zeilen
 * enthaelt. Eine undurchsichtige Kennung wuerde daran nichts verbessern und die Fehlersuche
 * erschweren.
 *
 * <h2>Form</h2>
 *
 * <p>{@code <MessageActionID>-<MessagePropertyName>}, also etwa {@code 0-FileReader.Payload.GUID}
 * oder {@code 2-FileReader.Log.GUID}. Zerlegt wird am <b>ersten</b> Bindestrich; der linke Teil
 * muss vollstaendig aus Ziffern bestehen, der rechte aus {@code A–Z}, {@code a–z}, {@code 0–9},
 * Punkt, Bindestrich und Unterstrich. Alle vorkommenden Zeichen sind in einem URL-Pfad
 * unreserviert, es wird also nichts kodiert. Was nicht passt, ist kein Artefakt und ergibt {@code
 * 404} — dieselbe Antwort wie eine unbekannte Kennung.
 *
 * <p><b>Das Beispiel hiess bis zum 19.08.2026 {@code 0-Message.Payload.GUID}.</b> Diese Kennung
 * zerfaellt weiterhin sauber, trifft aber keine Zeile mehr: Die Artefaktliste fuehrt {@code
 * Message.Payload.GUID} seit M73 nicht mehr ({@link Artefaktnamen#NAME_ZEIGER}). Als Beispiel fuer
 * eine gueltige Form taugt sie damit nicht.
 *
 * @param schritt {@code MessageActionID}. {@code 0} ist der Metadaten-Schritt; dort liegen die
 *     Zeilen des Lesedienstes, jede groessere Zahl ist ein Ablaufschritt
 * @param name {@code MessagePropertyName}, unveraendert
 */
public record ArtefaktId(short schritt, String name) {

  private static final char TRENNER = '-';

  public ArtefaktId {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("ArtefaktId ohne Namen gibt es nicht");
    }
    if (schritt < 0) {
      throw new IllegalArgumentException("ArtefaktId mit negativem Schritt gibt es nicht");
    }
  }

  /** Die Kennung, wie sie im Pfad steht. */
  public String kodiere() {
    return schritt + String.valueOf(TRENNER) + name;
  }

  /**
   * Zerlegt eine Kennung aus dem Pfad.
   *
   * <p><b>Streng und ohne Reparaturversuch.</b> Was nicht der Form entspricht, wird nicht
   * zurechtgebogen: Der Aufrufer bekommt {@link Optional#empty()}, und der Endpunkt macht daraus
   * dieselbe {@code 404} wie bei einer unbekannten Nachricht.
   */
  public static Optional<ArtefaktId> entschluessle(String kodiert) {
    if (kodiert == null || kodiert.isBlank()) {
      return Optional.empty();
    }
    int trenner = kodiert.indexOf(TRENNER);
    if (trenner <= 0 || trenner == kodiert.length() - 1) {
      return Optional.empty();
    }
    String schrittTeil = kodiert.substring(0, trenner);
    String nameTeil = kodiert.substring(trenner + 1);
    if (!nurZiffern(schrittTeil) || !zulaessigerName(nameTeil)) {
      return Optional.empty();
    }
    short schritt;
    try {
      schritt = Short.parseShort(schrittTeil);
    } catch (NumberFormatException zuGross) {
      // MessageActionID ist SMALLINT. Was nicht hineinpasst, kann keine Zeile treffen.
      return Optional.empty();
    }
    if (schritt < 0) {
      return Optional.empty();
    }
    return Optional.of(new ArtefaktId(schritt, nameTeil));
  }

  private static boolean nurZiffern(String wert) {
    // Bewusst nicht Character.isDigit: das liesse arabisch-indische Ziffern zu, die
    // Short.parseShort ebenfalls annimmt — und damit gaebe es zwei Schreibweisen derselben
    // Kennung. Eine Kennung, zwei Formen, ist genau das, was eine stabile Kennung nicht sein darf.
    if (wert.isEmpty() || wert.length() > 5) {
      return false;
    }
    for (int i = 0; i < wert.length(); i++) {
      char zeichen = wert.charAt(i);
      if (zeichen < '0' || zeichen > '9') {
        return false;
      }
    }
    return true;
  }

  private static boolean zulaessigerName(String wert) {
    // MessagePropertyName ist VARCHAR(100). Alles darueber kann keine Zeile treffen.
    if (wert.isEmpty() || wert.length() > 100) {
      return false;
    }
    for (int i = 0; i < wert.length(); i++) {
      char zeichen = wert.charAt(i);
      boolean erlaubt =
          (zeichen >= 'A' && zeichen <= 'Z')
              || (zeichen >= 'a' && zeichen <= 'z')
              || (zeichen >= '0' && zeichen <= '9')
              || zeichen == '.'
              || zeichen == '-'
              || zeichen == '_';
      if (!erlaubt) {
        return false;
      }
    }
    return true;
  }
}
