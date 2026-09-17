package de.kraftwerkone.overlord.monitor.common;

/**
 * Was der Baustein einem Verbraucher zurueckgibt: die Entscheidung und — nur bei {@code ANGEWANDT}
 * — die Korrektur samt der juengsten Live-Stunde je Prozess.
 *
 * <p>So geschnitten, dass Prozessbaum (Teil A) und Dashboard (Teil B) dasselbe Ergebnis nehmen: Der
 * Baum schneidet die Zeilen mit seinem Fenster, das Dashboard ordnet sie seinen Eimern zu. Beide
 * lesen die Rohwerte ueber {@code MessageStatusClassifier} ein, nicht hier.
 *
 * @param entscheidung der Zustand und seine Zeitpunkte
 * @param korrektur die Verrechnung; {@link LiveRestKorrektur#KEINE}, wenn nicht angewandt
 */
public record LiveRestErgebnis(LiveRestEntscheidung entscheidung, LiveRestKorrektur korrektur) {

  public LiveRestErgebnis {
    if (entscheidung == null || korrektur == null) {
      throw new IllegalArgumentException(
          "Ein Ergebnis ohne Entscheidung oder Korrektur gibt es nicht");
    }
    if (!entscheidung.angewandt() && !korrektur.zeilen().isEmpty()) {
      throw new IllegalArgumentException("Nur ANGEWANDT traegt Korrekturzeilen");
    }
  }

  /** Nichts zu verrechnen — fuer {@code NICHT_NOETIG} und {@code AUSGESETZT}. */
  public static LiveRestErgebnis ohneKorrektur(LiveRestEntscheidung entscheidung) {
    return new LiveRestErgebnis(entscheidung, LiveRestKorrektur.KEINE);
  }
}
