package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/**
 * Das <b>eine</b> Feld, das eine Massenzuordnung setzt.
 *
 * <p><b>Feldweise und nie beide zugleich</b> (E11) — und das ist gemessen und nicht bequem: Bei
 * {@code NEXANS} traegt das Projekt Richtung und Rolle ({@code 300_KundenEingehend}) und ist fuer
 * den Partner unbrauchbar, weil innerhalb eines Projekts durchgehend verschiedene Partner
 * nebeneinander stehen. Bei {@code VOTG} traegt das Projekt in 36 von 39 Faellen den Partner.
 * <i>Der Hebel greift — aber je Mandant fuer ein anderes Feld</i> (M76).
 */
public enum Zuordnungsfeld {
  PARTNER,
  RICHTUNG;

  /**
   * Liest das Feld aus einem Anfragefeld. Anders als bei {@link Richtung#ausText(String)} ist hier
   * <b>keine Angabe ein Fehler</b>: Eine Massenzuordnung ohne Feld hat keinen Gegenstand.
   */
  public static Zuordnungsfeld ausText(String text) {
    if (text != null && !text.isBlank()) {
      String gesucht = text.trim();
      for (Zuordnungsfeld feld : values()) {
        if (feld.name().equalsIgnoreCase(gesucht)) {
          return feld;
        }
      }
    }
    throw new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "feld-unbekannt",
        "Feld unbekannt",
        "Waehle genau eines der Felder " + werte() + ".",
        "Unbekanntes Feld in der Massenzuordnung");
  }

  private static String werte() {
    return Arrays.stream(values())
        .map(feld -> feld.name().toLowerCase(Locale.ROOT))
        .collect(Collectors.joining(", "));
  }
}
