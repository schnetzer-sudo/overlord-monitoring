package de.kraftwerkone.overlord.monitor.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * <b>Der Ersatz</b> — die reine Funktion von Fehler live, ohne Datenbank ({@code
 * docs/fehler-live.md} §4).
 *
 * <ol>
 *   <li>Aus den Grundzeilen fallen <b>alle</b> heraus, die {@link
 *       MessageStatusClassifier#einordnung(String)} als {@link MessageStatusKind#FEHLER} einordnet
 *       — auch ein unbekanntes {@code ERROR_} und {@code COMMIT_REJECTED}.
 *   <li>Die Zeilen der Live-Lesung kommen hinzu, <b>ueber denselben Schluessel</b>: Der Verbraucher
 *       hebt sie vorher auf die Gestalt seiner Grundzeilen (die Uebersicht auf ihre Eimer).
 * </ol>
 *
 * <p><b>Die Einordnung entsteht beim Lesen</b> (E-g): Die Zeilen tragen Rohwerte; welche Fehler
 * sind, entscheidet hier der Klassifizierer, und welche Fehlerart sie tragen, der Verbraucher ueber
 * denselben Klassifizierer.
 *
 * <p><b>Warum ersetzen und nicht verrechnen.</b> Der Live-Rest verrechnet, weil der Rollup vor G
 * stimmt und nur ab G etwas fehlt. Bei den Fehlern stimmt der Rollup auch vor G nicht: Ein Abgang
 * aus einem alten Eimer — eine nachverarbeitete Nachricht — bleibt dort bis zum Volllauf stehen,
 * und kein Delta-Lauf erreicht ihn. Die Fehlerzeilen des Rollups abzuziehen und die der Quelle
 * dazuzuzaehlen ergaebe je Schluessel genau die Zeilen der Quelle; ersetzen ist dieselbe Rechnung
 * ohne den Umweg.
 *
 * <p><b>Was gleich bleibt:</b> Jede Grundzeile, die kein Fehler ist, bleibt, wie sie ist —
 * einschliesslich der Zeilen, die der Live-Rest beigetragen hat. Ein Abgang aus einem anderen
 * Status ({@code RUNNING}, {@code SUSPENDED}, {@code FINISHED}) steht deshalb weiter bis zum
 * Volllauf in seinem alten Eimer; das ist eine benannte Grenze und kein Versehen.
 */
public final class FehlerLiveErsatz {

  private FehlerLiveErsatz() {}

  /**
   * Ersetzt die Fehlerzeilen.
   *
   * @param zustand {@code AUSGESETZT} laesst die Grundzeilen unveraendert
   * @param grundzeilen die Zeilen aus Rollup und Live-Rest, je Schluessel ein Rohwert
   * @param liveZeilen die Fehlerzeilen der Lesung, auf den Schluessel der Grundzeilen gehoben; leer
   *     bei {@code AUSGESETZT}
   * @param rohstatus der Rohwert einer Zeile
   * @param klassifizierer die eine Stelle, an der ein Rohwert eingeordnet wird
   * @return die Grundzeilen ohne Fehler, gefolgt von den Live-Zeilen
   */
  public static <Z> List<Z> ersetze(
      FehlerLiveZustand zustand,
      List<Z> grundzeilen,
      List<Z> liveZeilen,
      Function<? super Z, String> rohstatus,
      MessageStatusClassifier klassifizierer) {
    if (zustand == FehlerLiveZustand.AUSGESETZT) {
      if (!liveZeilen.isEmpty()) {
        throw new IllegalArgumentException("AUSGESETZT traegt keine Fehlerzeilen");
      }
      return grundzeilen;
    }
    List<Z> ersetzt = new ArrayList<>(grundzeilen.size() + liveZeilen.size());
    for (Z zeile : grundzeilen) {
      if (klassifizierer.einordnung(rohstatus.apply(zeile)) != MessageStatusKind.FEHLER) {
        ersetzt.add(zeile);
      }
    }
    ersetzt.addAll(liveZeilen);
    return List.copyOf(ersetzt);
  }
}
