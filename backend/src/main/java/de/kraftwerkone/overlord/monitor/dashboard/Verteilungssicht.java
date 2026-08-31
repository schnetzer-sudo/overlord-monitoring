package de.kraftwerkone.overlord.monitor.dashboard;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.util.Arrays;
import org.springframework.http.HttpStatus;

/**
 * Wonach der Verteilungsblock gruppiert — <b>ein Block, zwei Sichten</b>.
 *
 * <p>Beide laufen ueber <b>dasselbe Statement</b>, nur mit einer anderen Spalte im Ausdruck. Das
 * ist kein Zufall, sondern die Bedingung dafuer, dass die zweite Sicht nichts kostet, was die erste
 * nicht schon kostet: Der teure Teil ist der Bereichszugriff auf die Rolluptabelle, und der ist
 * derselbe.
 *
 * <p><b>Beide bekommen den {@code pflegestatus}-Riegel</b> (Entscheidung E-i). Bei der Richtung ist
 * er heute folgenlos — nach der Kuratierung tragen alle Zeilen mit Richtung {@code GEPFLEGT} —,
 * aber die Regel ist E-i und nicht der Zufall dieses Katalogstands.
 */
public enum Verteilungssicht {

  /** Nach kuratiertem Partner ({@code process_catalog.partner}). Die Vorgabe. */
  PARTNER,

  /** Nach kuratierter Richtung ({@code process_catalog.richtung}). */
  RICHTUNG;

  /** Die Sicht, die ohne Parameter gilt. */
  public static final Verteilungssicht VORGABE = PARTNER;

  /**
   * Der Wert aus der URL. {@code null} oder leer bedeutet {@link #VORGABE}.
   *
   * @throws FachlicheAusnahme {@code 400}, wenn der Wert keiner Sicht entspricht
   */
  public static Verteilungssicht ausCode(String code) {
    if (code == null || code.isBlank()) {
      return VORGABE;
    }
    return Arrays.stream(values())
        .filter(sicht -> sicht.name().equalsIgnoreCase(code.trim()))
        .findFirst()
        .orElseThrow(
            () ->
                new FachlicheAusnahme(
                    HttpStatus.BAD_REQUEST,
                    "verteilung-unbekannt",
                    "Verteilung unbekannt",
                    "Erlaubt sind PARTNER und RICHTUNG.",
                    "Unbekannte Verteilungssicht: " + code));
  }
}
