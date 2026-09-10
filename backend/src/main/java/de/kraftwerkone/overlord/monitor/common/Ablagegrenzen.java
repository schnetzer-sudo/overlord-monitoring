package de.kraftwerkone.overlord.monitor.common;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Die drei Grenzen, die der <b>Transport</b> zur Ablage braucht — und nur die drei.
 *
 * <h2>Warum sie hier stehen und nicht in {@code payload}</h2>
 *
 * <p>{@link SaajAblagezugriff} liegt seit Schritt 10d in {@code common}, weil ein zweiter
 * Verbraucher dazugekommen ist: die Ablagenpruefung des Dashboards. {@code common} darf {@code
 * payload} nicht kennen (Abschnitt 6 der Projektbeschreibung), also kommen die Werte von aussen —
 * ueber diese Bean, angemeldet in {@code config/RohdatenConfig}.
 *
 * <p><b>Die Schluessel bleiben unter {@code overlord.rohdaten.*} und keiner wird umbenannt.</b>
 * {@code payload/RohdatenEigenschaften} liest denselben Zweig weiter; die beiden Datensaetze
 * ueberschneiden sich in genau einem Wert, {@code maximalgroesse-bytes}, und der steht dort wie
 * hier fuer <i>dieselbe</i> Grenze: Der Transport bricht das Lesen des Anhangs daran ab, das
 * Entpacken in {@code payload} bricht den ZIP-Eintrag daran ab. Damit die Vorgabe nicht an zwei
 * Stellen auseinanderlaeuft, steht sie <b>einmal</b> — als {@link #VORGABE_MAXIMALGROESSE_BYTES}.
 *
 * <p><b>{@code anzeige-grenze-bytes} steht bewusst nicht hier.</b> Sie betrifft die Kappung der
 * Anzeige und nicht den Transport; sie waere genau das „mehr als noetig", das ein Umzug nach {@code
 * common} nicht mitnehmen soll.
 *
 * @param maximalgroesseBytes Die harte Obergrenze einer einzelnen Datei, in Bytes. Sie greift
 *     <b>waehrend</b> des Lesens und nicht davor: {@code FileReader.FileProperty.Size} deckt nur
 *     rund 69,6 % der Nachrichten in Fenster A (M56, Befund 1) und taugt deshalb nicht als
 *     Vorabpruefung. Gemessen ist ein groesstes Artefakt von <b>609.995 Byte</b>, keines ueber 1
 *     MiB (M60); die Vorgabe von 8 MiB liegt gut dreizehnfach darueber
 * @param verbindungszeitgrenze Zeitgrenze fuer den Verbindungsaufbau zur Ablage
 * @param lesezeitgrenze Zeitgrenze fuer die Antwort. Gemessen sind <b>38 bis 244 ms</b> je Abruf
 *     (M66, M60), der langsamste beobachtete Einzelabruf lag bei 497 ms (M71). 15 Sekunden liegen
 *     rund dreissigfach darueber und fangen den Fall ab, in dem ein Knoten annimmt und dann
 *     schweigt
 */
@ConfigurationProperties("overlord.rohdaten")
public record Ablagegrenzen(
    long maximalgroesseBytes, Duration verbindungszeitgrenze, Duration lesezeitgrenze) {

  /**
   * Die Vorgabe fuer {@code maximalgroesse-bytes}, an genau einer Stelle im Code.
   *
   * <p>{@code payload/RohdatenEigenschaften} liest denselben Schluessel und nimmt dieselbe Vorgabe
   * von hier. Zwei Literale waeren zwei Vorgaben, die auseinanderlaufen koennen, ohne dass es
   * jemandem auffaellt — die Konfiguration setzt den Wert ohnehin, und genau deshalb faellt ein
   * Auseinanderlaufen erst dann auf, wenn er einmal fehlt.
   */
  public static final long VORGABE_MAXIMALGROESSE_BYTES = 8L * 1024 * 1024;

  /** Vorgaben fuer den Fall, dass nichts konfiguriert ist. */
  public Ablagegrenzen {
    if (maximalgroesseBytes <= 0) {
      maximalgroesseBytes = VORGABE_MAXIMALGROESSE_BYTES;
    }
    if (verbindungszeitgrenze == null) {
      verbindungszeitgrenze = Duration.ofSeconds(5);
    }
    if (lesezeitgrenze == null) {
      lesezeitgrenze = Duration.ofSeconds(15);
    }
  }

  /**
   * Die Lesezeitgrenze in Millisekunden, wie {@code SOAPConnection.setReadTimeout} sie erwartet.
   *
   * <p>Auf {@link Integer#MAX_VALUE} begrenzt, weil die Schnittstelle {@code int} nimmt. Eine
   * Zeitgrenze, die durch einen Ueberlauf negativ wuerde, waere schlimmer als gar keine.
   */
  public int lesezeitgrenzeMillis() {
    return millis(lesezeitgrenze);
  }

  /** Die Verbindungszeitgrenze in Millisekunden. */
  public int verbindungszeitgrenzeMillis() {
    return millis(verbindungszeitgrenze);
  }

  private static int millis(Duration dauer) {
    long wert = dauer.toMillis();
    return wert > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) wert;
  }
}
