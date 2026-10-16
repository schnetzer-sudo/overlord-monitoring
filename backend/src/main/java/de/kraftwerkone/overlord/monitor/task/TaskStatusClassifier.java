package de.kraftwerkone.overlord.monitor.task;

import java.util.List;
import java.util.Optional;

/**
 * <b>Die eine Stelle</b>, an der aus gespeichertem Status und Rueckgabe der Gesamtstand eines
 * Auftrags wird (E‑1009, {@code docs/tasks.md} §5.5).
 *
 * <p>„Teilweise erledigt“ wird nicht gespeichert, sondern hier abgeleitet: Die Plattform setzt nur
 * {@code ERLEDIGT} oder {@code FEHLGESCHLAGEN} ({@code docs/task-schnittstelle.md} §5). Steht es an
 * einer zweiten Stelle, laufen die beiden auseinander — deshalb liest auch die Liste die Menge der
 * laufenden Status von hier ({@link #LAUFEND}).
 */
public final class TaskStatusClassifier {

  public static final String OFFEN = "OFFEN";
  public static final String IN_AUSFUEHRUNG = "IN_AUSFUEHRUNG";
  public static final String ERLEDIGT = "ERLEDIGT";
  public static final String FEHLGESCHLAGEN = "FEHLGESCHLAGEN";

  /**
   * Die gespeicherten Status, deren Auftraege noch laufen. {@code GET /api/tasks} liefert sie
   * unabhaengig vom Alter.
   */
  public static final List<String> LAUFEND = List.of(OFFEN, IN_AUSFUEHRUNG);

  private TaskStatusClassifier() {}

  /**
   * @param status der gespeicherte Status, roh; verglichen wird genau
   * @param rueckgabe gelesen gegen die Kopie der Definition; leer, wenn sie fehlt
   * @param eintraege die Zahl der Eintraege des Auftrags
   */
  public static Einordnung einordnen(String status, Optional<Rueckgabe> rueckgabe, int eintraege) {
    if (status == null) {
      return Einordnung.UNBEKANNT;
    }
    return switch (status) {
      case OFFEN -> Einordnung.OFFEN;
      case IN_AUSFUEHRUNG -> Einordnung.IN_AUSFUEHRUNG;
      case FEHLGESCHLAGEN -> Einordnung.FEHLGESCHLAGEN;
      case ERLEDIGT -> erledigt(rueckgabe, eintraege);
      default -> Einordnung.UNBEKANNT;
    };
  }

  private static Einordnung erledigt(Optional<Rueckgabe> rueckgabe, int eintraege) {
    if (rueckgabe.isEmpty() || rueckgabe.get().zeilen().size() != eintraege) {
      return Einordnung.UNSTIMMIG;
    }
    return rueckgabe.get().alleOk() ? Einordnung.ERLEDIGT : Einordnung.TEILWEISE_ERLEDIGT;
  }
}
