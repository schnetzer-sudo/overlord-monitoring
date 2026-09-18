package de.kraftwerkone.overlord.monitor.common;

import java.util.List;

/**
 * Was der Baustein einem Verbraucher zurueckgibt: den Zustand und — nur bei {@code ANGEWANDT} — die
 * Fehlerzeilen des Fensters ({@code docs/fehler-live.md} §4).
 *
 * <p>So geschnitten wie {@link LiveRestErgebnis}: Die Uebersicht hebt die Zeilen auf ihre Eimer,
 * der Prozessbaum (Teil B) wird sie je Prozess summieren. Beide ersetzen damit ihre Fehlerzeilen
 * ueber {@link FehlerLiveErsatz} und ordnen die Rohwerte ueber {@code MessageStatusClassifier} ein,
 * nicht hier.
 *
 * @param zustand angewandt oder ausgesetzt
 * @param zeilen die Fehlerzeilen des Fensters; leer bei {@code AUSGESETZT} — und bei {@code
 *     ANGEWANDT}, wenn im Fenster kein Fehler steht. <b>Leer und angewandt heisst: kein Fehler</b>,
 *     nicht: nicht gelesen
 */
public record FehlerLiveErgebnis(FehlerLiveZustand zustand, List<FehlerLiveZeile> zeilen) {

  public FehlerLiveErgebnis {
    if (zustand == null || zeilen == null) {
      throw new IllegalArgumentException("Ein Ergebnis ohne Zustand oder Zeilen gibt es nicht");
    }
    zeilen = List.copyOf(zeilen);
    if (zustand == FehlerLiveZustand.AUSGESETZT && !zeilen.isEmpty()) {
      throw new IllegalArgumentException("Nur ANGEWANDT traegt Fehlerzeilen");
    }
  }

  /** Die Lesung ist gelaufen — mit ihren Zeilen, auch wenn es keine gibt. */
  public static FehlerLiveErgebnis angewandt(List<FehlerLiveZeile> zeilen) {
    return new FehlerLiveErgebnis(FehlerLiveZustand.ANGEWANDT, zeilen);
  }

  /** Die Lesung ist ausgefallen; die Fehler bleiben beim Rollup. */
  public static FehlerLiveErgebnis ausgesetzt() {
    return new FehlerLiveErgebnis(FehlerLiveZustand.AUSGESETZT, List.of());
  }

  /** Ersetzt ein Verbraucher seine Fehlerzeilen? */
  public boolean angewandt() {
    return zustand == FehlerLiveZustand.ANGEWANDT;
  }
}
