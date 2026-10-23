package de.kraftwerkone.overlord.monitor.assistent;

import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.Feld;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ein gueltiger Vorschlag fuer die Karte (E‑1041, {@code docs/assistent.md} §4.7). Gueltig heisst:
 * {@code AuftragsPruefung} hat nichts gefunden.
 *
 * <p><b>Jedes Feld, das bei der Aktion vorkommt, steht in jedem Eintrag</b> — leere ausdruecklich
 * als {@code null} —, und {@link #felder()} nennt je Feld {@code label} und {@code hinweis}. So
 * kann die Karte zeigen, was ein leeres Feld bedeutet (etwa „wird entfernt“, wie es der Hinweis
 * eines Felds sagt), ohne einen Typ zu kennen.
 *
 * <p><b>Er legt nichts an.</b> Anlegen tut der Klick ueber {@code POST /api/tasks}.
 */
public record Vorschlag(
    String typ, String name, String aktion, List<FeldInfo> felder, List<Eintrag> eintraege) {

  /** Ein Feld, das bei der Aktion vorkommt, mit seiner Bedeutung aus der Definition. */
  public record FeldInfo(String name, String label, String hinweis) {}

  /**
   * Ein Eintrag.
   *
   * @param werte jedes Feld aus {@link #felder()}, in deren Reihenfolge; leer als {@code null}
   * @param schluessel der Schluessel nach der Definition, {@code null} ohne Schluessel
   */
  public record Eintrag(Map<String, String> werte, String schluessel) {}

  /**
   * Der Vorschlag aus den Eintraegen, wie die Pruefung sie zurueckgibt — mit gekuerzten
   * Schluesselteilen ({@code AuftragsPruefung.Ergebnis}). Hier wird kein Wert veraendert.
   */
  static Vorschlag aus(TypDefinition definition, String aktion, List<Map<String, String>> liste) {
    List<Feld> vorkommend =
        definition.felder().stream().filter(feld -> feld.kommtVorBei(aktion)).toList();
    List<FeldInfo> felder =
        vorkommend.stream()
            .map(feld -> new FeldInfo(feld.name(), feld.label(), feld.hinweis()))
            .toList();
    List<Eintrag> eintraege = new ArrayList<>();
    for (Map<String, String> eintrag : liste) {
      Map<String, String> werte = new LinkedHashMap<>();
      for (Feld feld : vorkommend) {
        String wert = eintrag.get(feld.name());
        werte.put(feld.name(), TypDefinition.hatWert(wert) ? wert : null);
      }
      eintraege.add(
          new Eintrag(Collections.unmodifiableMap(werte), definition.schluesselText(werte)));
    }
    return new Vorschlag(definition.typ(), definition.name(), aktion, felder, eintraege);
  }
}
