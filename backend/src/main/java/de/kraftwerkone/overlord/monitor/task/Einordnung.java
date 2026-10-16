package de.kraftwerkone.overlord.monitor.task;

/**
 * Der Gesamtstand eines Auftrags, wie der Kunde ihn sieht ({@code docs/tasks.md} §5.5). Entsteht
 * ausschliesslich im {@link TaskStatusClassifier}.
 */
public enum Einordnung {
  OFFEN,
  IN_AUSFUEHRUNG,
  /** Alle Eintraege Ok. */
  ERLEDIGT,
  /** Mindestens ein Eintrag nicht Ok — auch dann, wenn keiner Ok ist (Punkt 1000). */
  TEILWEISE_ERLEDIGT,
  /**
   * {@code ERLEDIGT} gemeldet, aber die Rueckgabe fehlt oder passt nicht zur Zahl der Eintraege.
   */
  UNSTIMMIG,
  FEHLGESCHLAGEN,
  /** Ein gespeicherter Status, den der Vertrag nicht kennt; der Rohwert steht in {@code status}. */
  UNBEKANNT
}
