package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/**
 * Die Richtung einer EDI-Uebertragung aus Sicht des Mandanten — eine geschlossene Menge aus zwei
 * Werten ({@code docs/prozess-katalog.md} §2).
 *
 * <p><b>Bewusst kein {@code ENUM} in der Datenbank</b>, sondern {@code varchar(20)} mit dieser
 * Whitelist im Code: ein weiterer Wert soll keine Migration kosten. Dieselbe Entscheidung wie bei
 * {@code audit_log.event_type} und {@code app_user.role}.
 */
public enum Richtung {
  EINGEHEND,
  AUSGEHEND;

  /**
   * Liest die Richtung aus einem Anfragefeld.
   *
   * <p><b>Leer ist eine Angabe, kein Fehler:</b> {@code null} oder Leerraum heisst „keine Richtung"
   * und ist ein gueltiger gepflegter Zustand — dieselbe Ueberlegung wie beim leeren Partner (E4).
   * Ein <i>unbekannter</i> Wert ist dagegen 400 und faellt nicht stillschweigend auf einen
   * Vorgabewert; sonst bliebe ein Tippfehler unbemerkt.
   *
   * @return die Richtung oder {@code null}, wenn keine angegeben ist
   */
  public static Richtung ausText(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String gesucht = text.trim();
    for (Richtung richtung : values()) {
      if (richtung.name().equalsIgnoreCase(gesucht)) {
        return richtung;
      }
    }
    throw new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "richtung-unbekannt",
        "Richtung unbekannt",
        "Waehle eine der Richtungen " + werte() + " — oder lass das Feld leer.",
        "Unbekannte Richtung in der Katalogpflege");
  }

  private static String werte() {
    return Arrays.stream(values())
        .map(richtung -> richtung.name().toLowerCase(Locale.ROOT))
        .collect(Collectors.joining(", "));
  }
}
