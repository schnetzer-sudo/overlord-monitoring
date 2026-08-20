package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/**
 * Die zwei Modi der Massenzuordnung.
 *
 * <p><b>Die Vorschau ist Pflicht und kein Komfort.</b> Das groesste Projekt im Bestand traegt 226
 * Prozesse ({@code VOTG}, {@code 110_VTG_SalesInvoice}), das groesste bei {@code NEXANS} 161 (M76).
 * Und die Zuordnung <b>ueberschreibt gepflegte Zeilen</b> (E12) — der Schutzmodus „nur offene
 * Zeilen" machte genau die Korrektur unmoeglich, fuer die man sie braucht.
 *
 * <p>Deshalb nennt die Vorschau nicht nur die Zahl der betroffenen Zeilen, sondern auch, wie viele
 * davon bereits gepflegt sind: Das ist die Zahl, die verloren geht.
 */
public enum Massenmodus {
  VORSCHAU,
  AUSFUEHREN;

  /**
   * Liest den Modus aus einem Anfragefeld.
   *
   * <p><b>Ohne Angabe gilt {@link #VORSCHAU}.</b> Die harmlose Variante ist die Vorgabe: Wer den
   * Modus vergisst, veraendert nichts. Ein <i>unbekannter</i> Wert ist dagegen 400 und faellt nicht
   * stillschweigend auf die Vorgabe — sonst schriebe ein Tippfehler in {@code AUSFUEREN} nichts und
   * niemand wuesste warum.
   */
  public static Massenmodus ausText(String text) {
    if (text == null || text.isBlank()) {
      return VORSCHAU;
    }
    String gesucht = text.trim();
    for (Massenmodus modus : values()) {
      if (modus.name().equalsIgnoreCase(gesucht)) {
        return modus;
      }
    }
    throw new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "modus-unbekannt",
        "Modus unbekannt",
        "Waehle einen der Modi " + werte() + ".",
        "Unbekannter Modus in der Massenzuordnung");
  }

  private static String werte() {
    return Arrays.stream(values())
        .map(modus -> modus.name().toLowerCase(Locale.ROOT))
        .collect(Collectors.joining(", "));
  }
}
