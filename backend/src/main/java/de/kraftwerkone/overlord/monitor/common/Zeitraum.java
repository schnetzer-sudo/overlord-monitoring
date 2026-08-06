package de.kraftwerkone.overlord.monitor.common;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/**
 * Die waehlbaren <b>relativen</b> Zeitraeume eines Listen-Endpunkts.
 *
 * <p><b>Relative Zeitraeume werden niemals im Frontend gerechnet.</b> Der Aufrufer schickt {@code
 * 24h}, nicht zwei Zeitpunkte: Kaeme das Fenster aus der Browseruhr, waere die Anwendungsuhr aus
 * {@code common/ZeitConfig} umgangen — und die Liste lokal immer leer, weil die Testkopie hinter
 * der realen Uhrzeit liegt (Regel Z1).
 *
 * <p>Bewusst eine kleine, geschlossene Auswahl statt einer freien Dauer. Wer ein anderes Fenster
 * braucht, nennt {@code von} und {@code bis} — das ist der zweite, absolute Modus.
 */
public enum Zeitraum {
  LETZTE_24_STUNDEN("24h", Duration.ofHours(24)),
  LETZTE_7_TAGE("7d", Duration.ofDays(7)),
  LETZTE_30_TAGE("30d", Duration.ofDays(30));

  private final String code;
  private final Duration dauer;

  Zeitraum(String code, Duration dauer) {
    this.code = code;
    this.dauer = dauer;
  }

  /** Der Wert, wie er in der URL steht. */
  public String code() {
    return code;
  }

  public Duration dauer() {
    return dauer;
  }

  /**
   * @throws FachlicheAusnahme {@code 400}, wenn der Code unbekannt ist. Ein unbekannter Wert wird
   *     <b>nicht</b> stillschweigend auf die Vorgabe gezogen: Wer {@code zeitraum=24} schreibt,
   *     bekaeme sonst 24 Stunden und haette keinen Anlass, den Tippfehler zu bemerken.
   */
  public static Zeitraum ausCode(String code) {
    for (Zeitraum zeitraum : values()) {
      if (zeitraum.code.equalsIgnoreCase(code)) {
        return zeitraum;
      }
    }
    throw new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "zeitraum-unbekannt",
        "Zeitraum unbekannt",
        "Waehle einen der Zeitraeume " + codes() + ".",
        "Unbekannter Zeitraum-Code");
  }

  private static String codes() {
    return Arrays.stream(values()).map(Zeitraum::code).collect(Collectors.joining(", "));
  }
}
