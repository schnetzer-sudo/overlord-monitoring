package de.kraftwerkone.overlord.monitor.common;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/**
 * Die Richtung der Sortierung ueber den Zeitpunkt.
 *
 * <p><b>Es gibt keinen zweiten Sortierschluessel.</b> Cursor-Paginierung funktioniert nur auf dem
 * Sortierschluessel selbst, und fuer Status oder Prozessname gibt es weder einen Index noch einen
 * eindeutigen Tiebreaker — nach Status sortiert waere die Reihenfolge innerhalb einer Statusgruppe
 * zufaellig, und ein Cursor darauf wuerde Zeilen ueberspringen oder doppelt liefern.
 */
public enum Sortierrichtung {
  NEUESTE("neueste"),
  AELTESTE("aelteste");

  private final String code;

  Sortierrichtung(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  /** Neueste zuerst heisst absteigend. */
  public boolean absteigend() {
    return this == NEUESTE;
  }

  /**
   * @throws FachlicheAusnahme {@code 400} bei unbekanntem Wert — nicht stillschweigend die Vorgabe
   */
  public static Sortierrichtung ausCode(String code) {
    for (Sortierrichtung richtung : values()) {
      if (richtung.code.equalsIgnoreCase(code)) {
        return richtung;
      }
    }
    throw new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "sortierung-unbekannt",
        "Sortierung unbekannt",
        "Waehle eine der Sortierungen " + codes() + ".",
        "Unbekannter Sortierungs-Code");
  }

  private static String codes() {
    return Arrays.stream(values()).map(Sortierrichtung::code).collect(Collectors.joining(", "));
  }
}
