package de.kraftwerkone.overlord.monitor.task;

import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import java.util.List;

/**
 * Ein angebotener Auftragstyp in {@code GET /api/tasks/typen} — die Grundlage fuer Assistent (A2)
 * und Karte (A3).
 *
 * <p><b>{@code regeln}, {@code zeile} und {@code rueckgaben} gehen nicht hinaus.</b> Den
 * Parametertext baut das Backend (E‑1006), und die Regeln prueft es selbst; der Assistent erfaehrt
 * sie aus der Beschreibung und aus den Fehlercodes der Pruefung.
 *
 * @param schluessel {@code null}, wenn der Typ keinen hat
 */
public record TypResponse(
    String typ,
    String name,
    String beschreibung,
    List<String> aktionen,
    int maxEintraege,
    SchluesselResponse schluessel,
    List<FeldResponse> felder) {

  public record SchluesselResponse(List<String> felder, String trenner) {}

  /**
   * @param nurBei {@code null} heisst: bei allen Aktionen
   */
  public record FeldResponse(
      String name,
      String label,
      boolean pflicht,
      List<String> nurBei,
      String muster,
      String beispiel,
      String hinweis) {}

  static TypResponse aus(TypDefinition definition) {
    return new TypResponse(
        definition.typ(),
        definition.name(),
        definition.beschreibung(),
        definition.aktionen(),
        definition.maxEintraege(),
        definition.schluessel() == null
            ? null
            : new SchluesselResponse(
                definition.schluessel().felder(), definition.schluessel().trenner()),
        definition.felder().stream()
            .map(
                feld ->
                    new FeldResponse(
                        feld.name(),
                        feld.label(),
                        feld.pflicht(),
                        feld.nurBei(),
                        feld.muster(),
                        feld.beispiel(),
                        feld.hinweis()))
            .toList());
  }
}
