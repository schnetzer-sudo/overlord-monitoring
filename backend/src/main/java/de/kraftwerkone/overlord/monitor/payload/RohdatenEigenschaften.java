package de.kraftwerkone.overlord.monitor.payload;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Die Grenzen des Rohdatenzugriffs. Alle vier Werte haben eine gemessene Bemessungsgrundlage und
 * keinen geratenen Rundwert.
 *
 * @param maximalgroesseBytes Die harte Obergrenze einer einzelnen Datei, in Bytes. Sie greift
 *     <b>waehrend</b> des Lesens und nicht davor: {@code FileReader.FileProperty.Size} deckt nur
 *     rund 69,6 % der Artefakte (M17) und taugt deshalb nicht als Vorabpruefung (M60). Gemessen ist
 *     ein groesstes Artefakt von <b>609.995 Byte</b>, keines ueber 1 MiB; die Vorgabe von 8 MiB
 *     liegt gut dreizehnfach darueber. Sie ist eine Schutzmassnahme gegen die Produktion, die nicht
 *     die Testkopie ist — kein Regelfall
 * @param anzeigeGrenzeBytes Ab wie vielen Bytes die <b>Anzeige</b> gekappt wird, mit sichtbarem
 *     Hinweis. Bewusst kleiner als {@link #maximalgroesseBytes()}: Der Download soll die ganze
 *     Datei liefern koennen, auch wenn im Browser nur der Anfang steht. 1 MiB liegt ueber dem
 *     gemessenen Maximum, die Kappung greift im gemessenen Bestand also nie
 * @param verbindungszeitgrenze Zeitgrenze fuer den Verbindungsaufbau zur Ablage
 * @param lesezeitgrenze Zeitgrenze fuer die Antwort. Gemessen sind <b>38 bis 244 ms</b> je Abruf
 *     (M66, M60), der langsamste beobachtete Einzelabruf lag bei 497 ms (M71). 15 Sekunden liegen
 *     rund dreissigfach darueber und fangen den Fall ab, in dem ein Knoten annimmt und dann
 *     schweigt
 */
@ConfigurationProperties("overlord.rohdaten")
public record RohdatenEigenschaften(
    long maximalgroesseBytes,
    long anzeigeGrenzeBytes,
    Duration verbindungszeitgrenze,
    Duration lesezeitgrenze) {

  /** Vorgaben fuer den Fall, dass nichts konfiguriert ist. */
  public RohdatenEigenschaften {
    if (maximalgroesseBytes <= 0) {
      maximalgroesseBytes = 8L * 1024 * 1024;
    }
    if (anzeigeGrenzeBytes <= 0) {
      anzeigeGrenzeBytes = 1024L * 1024;
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
