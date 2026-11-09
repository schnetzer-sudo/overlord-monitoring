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
  /** Mindestens ein Eintrag Ok und mindestens einer nicht Ok (E‑1009, E‑1182). */
  TEILWEISE_ERLEDIGT,
  /**
   * {@code ERLEDIGT} gemeldet, aber die Rueckgabe fehlt oder passt nicht zur Zahl der Eintraege.
   */
  UNSTIMMIG,
  /**
   * Entsteht auf zwei Wegen (E‑1182): von der Plattform gesetzt, dann mit {@code fehlertext}; oder
   * abgeleitet aus {@code ERLEDIGT} mit passender Zeilenzahl und keiner Zeile Ok, dann ohne {@code
   * fehlertext} und mit dem Ergebnis je Eintrag.
   */
  FEHLGESCHLAGEN,
  /** Ein gespeicherter Status, den der Vertrag nicht kennt; der Rohwert steht in {@code status}. */
  UNBEKANNT
}
