package de.kraftwerkone.overlord.monitor.payload;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Der <b>Download</b> kann nicht liefern, weil es nichts zu liefern gibt.
 *
 * <h2>Warum nur der Download und nicht die Anzeige</h2>
 *
 * <p>Die Anzeige antwortet in allen vier Zustaenden mit {@code 200} und dem benannten Zustand —
 * genau so will es {@code docs/rohdaten.md} §8: „Keiner davon ist ein leeres Feld." Ein {@code 404}
 * fuer ein Protokoll ohne Marken waere fuer den Nutzer nicht von „die Nachricht gibt es nicht" zu
 * unterscheiden, und das ist etwas voellig anderes.
 *
 * <p>Der Download hat diese Moeglichkeit nicht. Eine Datei mit null Byte auszuliefern waere formal
 * der Gleichlauf mit der Anzeige, praktisch aber eine irrefuehrende Antwort: Der Nutzer bekaeme
 * eine Datei, die nach einem Fehler seines Rechners aussieht. Deshalb ein Fehlerrumpf nach RFC 9457
 * — mit eigenem Problemtyp je Zustand, damit die Oberflaeche dieselben vier Texte verwenden kann
 * wie bei der Anzeige.
 *
 * <h2>Die drei Statuscodes</h2>
 *
 * <table border="1">
 *   <caption>Zustand, Status und Problemtyp</caption>
 *   <tr><th>Zustand</th><th>Status</th><th>warum</th></tr>
 *   <tr><td>{@link Artefaktzustand#KEIN_ANZEIGBARER_PROTOKOLLTEIL}</td><td>{@code 409}</td>
 *       <td>Die Datei ist da, sie hat nur keinen Teil, den dieser Aufrufer bekommt. Kein Fehler
 *       irgendeiner Seite, sondern eine Regel</td></tr>
 *   <tr><td>{@link Artefaktzustand#DATEI_NICHT_VORHANDEN}</td><td>{@code 404}</td>
 *       <td>Die Ablage hat geantwortet und nichts geliefert. Die Datei gibt es nicht</td></tr>
 *   <tr><td>{@link Artefaktzustand#ABLAGE_NICHT_ERREICHBAR}</td><td>{@code 502}</td>
 *       <td>Eine fremde Anlage antwortet nicht. Das ist kein Fehler des Aufrufers, und ein erneuter
 *       Versuch kann helfen — beides sagt {@code 4xx} nicht</td></tr>
 * </table>
 *
 * <p><b>Das {@code 404} hier ist von dem der unbekannten Nachricht zu unterscheiden</b>, und das
 * ist unbedenklich: Diesen Problemtyp bekommt nur, wer die Nachricht ohnehin sehen darf. Wer eine
 * fremde {@code MessageID} raet, kommt gar nicht bis hierher — er bekommt {@code nicht-gefunden}
 * aus {@code RessourceNichtGefundenException}, wie bei jedem anderen Endpunkt auch.
 */
public class AbrufFehlgeschlagenException extends FachlicheAusnahme {

  public AbrufFehlgeschlagenException(Artefaktzustand zustand) {
    super(
        status(zustand),
        problemTyp(zustand),
        titel(zustand),
        text(zustand),
        "Rohdatenabruf ohne lieferbaren Inhalt: " + zustand,
        Map.of("zustand", zustand.name()));
  }

  private static HttpStatus status(Artefaktzustand zustand) {
    return switch (zustand) {
      case KEIN_ANZEIGBARER_PROTOKOLLTEIL, BINAERDATEI -> HttpStatus.CONFLICT;
      case DATEI_NICHT_VORHANDEN -> HttpStatus.NOT_FOUND;
      case ABLAGE_NICHT_ERREICHBAR -> HttpStatus.BAD_GATEWAY;
      // ANZEIGBAR liefert immer Bytes und landet hier nie. Der Zweig existiert, weil ein
      // stillschweigendes 500 der schlechtere Weg waere, einen kuenftigen sechsten Zustand zu
      // entdecken.
      case ANZEIGBAR -> throw new IllegalArgumentException("Zustand mit Inhalt: " + zustand);
    };
  }

  private static String problemTyp(Artefaktzustand zustand) {
    return switch (zustand) {
      case KEIN_ANZEIGBARER_PROTOKOLLTEIL -> "kein-anzeigbarer-protokollteil";
      case BINAERDATEI -> "binaeres-protokoll";
      case DATEI_NICHT_VORHANDEN -> "datei-nicht-vorhanden";
      case ABLAGE_NICHT_ERREICHBAR -> "ablage-nicht-erreichbar";
      case ANZEIGBAR -> throw new IllegalArgumentException("Zustand mit Inhalt: " + zustand);
    };
  }

  private static String titel(Artefaktzustand zustand) {
    return switch (zustand) {
      case KEIN_ANZEIGBARER_PROTOKOLLTEIL -> "Kein anzeigbarer Protokollteil";
      case BINAERDATEI -> "Protokoll nicht lesbar";
      case DATEI_NICHT_VORHANDEN -> "Datei nicht vorhanden";
      case ABLAGE_NICHT_ERREICHBAR -> "Ablage nicht erreichbar";
      case ANZEIGBAR -> throw new IllegalArgumentException("Zustand mit Inhalt: " + zustand);
    };
  }

  /**
   * Die Texte an den Nutzer. Sie nennen weder eine Ablage noch eine Adresse noch eine Kennung — der
   * {@code ServiceConnectString} traegt einen Hostnamen (Regel G1) und erscheint in keiner Antwort.
   */
  private static String text(Artefaktzustand zustand) {
    return switch (zustand) {
      case KEIN_ANZEIGBARER_PROTOKOLLTEIL ->
          "Dieses Protokoll enthaelt keinen Abschnitt, der fuer dich freigegeben ist.";
      case BINAERDATEI ->
          "Dieses Protokoll ist keine Textdatei und laesst sich deshalb nicht auf den fuer dich"
              + " freigegebenen Abschnitt eingrenzen.";
      case DATEI_NICHT_VORHANDEN ->
          "Zu diesem Eintrag liegt keine Datei mehr vor. Ihre Aufbewahrungsfrist ist"
              + " moeglicherweise abgelaufen.";
      case ABLAGE_NICHT_ERREICHBAR ->
          "Die Dateiablage antwortet gerade nicht. Bitte versuche es spaeter noch einmal.";
      case ANZEIGBAR -> throw new IllegalArgumentException("Zustand mit Inhalt: " + zustand);
    };
  }
}
