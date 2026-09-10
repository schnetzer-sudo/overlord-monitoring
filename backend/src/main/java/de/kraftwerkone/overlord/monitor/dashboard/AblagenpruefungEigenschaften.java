package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Die zwei Stellschrauben der Ablagenpruefung. Sie stehen in {@code application.yml} und nicht als
 * Literal im Code, damit im Betrieb sichtbar ist, was gilt, ohne dass jemand eine Klasse
 * aufschlagen muss. Es sind keine Zugangsdaten; sie duerfen deshalb in der versionierten Datei
 * stehen.
 *
 * <table border="1">
 *   <caption>Alle Schluessel unter {@code overlord.ablagenpruefung}</caption>
 *   <tr><th>Schluessel</th><th>Vorgabe</th><th>Wirkung</th></tr>
 *   <tr><td>{@code aktiv}</td><td>{@code false}, wenn nicht gesetzt
 *       ({@code application.yml}: {@code true}, im {@code dev}-Block {@code false})</td>
 *       <td>die zeitgesteuerte Pruefung an/aus</td></tr>
 *   <tr><td>{@code takt}</td><td>{@code 60s}</td>
 *       <td>Abstand zwischen zwei Durchgaengen — <b>gesetzt, nicht gemessen</b></td></tr>
 * </table>
 *
 * @param aktiv Ob die zeitgesteuerte Pruefung laeuft. <b>Fehlt der Schluessel, ist sie AUS</b> —
 *     ein {@code boolean} ohne Angabe ist {@code false}, und das ist hier die richtige Richtung:
 *     Ein Lauf, der wegen eines vergessenen Schluessels unaufgefordert fremde Knoten anspricht,
 *     waere genau die Ueberraschung, die dieser Riegel verhindern soll. Dieselbe Entscheidung wie
 *     bei {@code overlord.rollup.aktiv} ({@code docs/rollup.md} §8).
 *     <p><b>Ist sie aus, gibt es {@link Ablagenpruefung} gar nicht</b>, und die Kachel sagt {@link
 *     Ablagengrund#ABGESCHALTET} — mit benanntem Grund und nicht als leeres Feld.
 * @param takt Der Abstand zwischen dem <b>Ende</b> eines Durchgangs und dem Beginn des naechsten
 *     ({@code fixedDelay}, nicht {@code fixedRate} — ein Durchgang ueberholt den naechsten nie).
 *     <p><b>Der Wert ist gesetzt und nicht gemessen (Regel Q4).</b> Wie oft eine Ablage ausfaellt
 *     und wie schnell das jemand wissen muss, ist nicht erhoben. Gemessen ist nur, was ein
 *     Durchgang <i>kostet</i>: rund 130 ms je erreichbarem und rund 2,7 s je abgeschaltetem Ziel
 *     (M174). Bei einer Minute Abstand ist selbst der schlechte Fall weit vom Dauerbetrieb
 *     entfernt.
 *     <p>Er wird zusaetzlich als Platzhalter in {@code @Scheduled} gelesen; dieser Datensatz traegt
 *     ihn, weil die Kachel ihn braucht — sie erklaert einen Stand fuer veraltet, sobald er aelter
 *     ist als <b>zwei</b> Takte
 */
@ConfigurationProperties("overlord.ablagenpruefung")
public record AblagenpruefungEigenschaften(boolean aktiv, Duration takt) {

  /** Die Vorgabe fuer {@code takt}, an genau einer Stelle im Code. */
  public static final Duration VORGABE_TAKT = Duration.ofSeconds(60);

  /** Vorgaben fuer den Fall, dass nichts konfiguriert ist. */
  public AblagenpruefungEigenschaften {
    if (takt == null || takt.isZero() || takt.isNegative()) {
      takt = VORGABE_TAKT;
    }
  }
}
