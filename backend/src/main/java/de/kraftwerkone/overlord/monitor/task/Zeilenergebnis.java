package de.kraftwerkone.overlord.monitor.task;

/**
 * Wie eine Zeile der Rueckgabe gegen die Kopie der Definition steht ({@code docs/tasks.md} §5.4).
 */
public enum Zeilenergebnis {
  /** gleich {@code rueckgaben.ok} */
  OK,
  /** in {@code rueckgaben.weitere} */
  NICHT_OK,
  /** jeder andere Wert — zaehlt als nicht Ok, der Rohwert steht daneben */
  UNBEKANNT
}
