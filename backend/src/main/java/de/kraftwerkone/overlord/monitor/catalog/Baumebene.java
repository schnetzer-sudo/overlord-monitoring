package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Der Name einer Ebene des Prozessbaums — so, wie er in {@code ebenen} der Antwort steht.
 *
 * <p><i>(seit 15.09.2026.)</i> Die Antwort nennt ihre Ebenen <b>von aussen nach innen</b>, damit
 * die Oberflaeche die Tiefe nicht aus der Gliederung ableiten muss: {@code ["PARTNER", "RICHTUNG",
 * "PROZESS"]} fuer {@code PARTNER}, {@code ["PROJEKT", "PROZESS"]} fuer {@code PROJEKT}. Die
 * innerste Ebene ist immer {@link #PROZESS} — beide Gliederungen enden im selben Blatt.
 *
 * <p><b>Er steht in {@code catalog} und nicht in {@code common}</b>: Anders als {@code
 * common/Baumgliederung} braucht ihn kein zweites Fachpaket. Die Vorgabe je Konto kennt nur die
 * Gliederung, nicht ihre Ebenen.
 */
public enum Baumebene {
  PARTNER,
  RICHTUNG,
  PROJEKT,
  PROZESS
}
