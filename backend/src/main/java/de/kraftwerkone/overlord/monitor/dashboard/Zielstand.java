package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Was ein einzelnes Ziel im letzten Durchgang geantwortet hat.
 *
 * <p><b>Er traegt die Kennung und nichts sonst</b> — insbesondere keine Adresse und keine
 * Rohantwort. Was die Ablage ueber sich selbst sagt ({@code Response=Error (Skipped)}), ist eine
 * Auskunft der fremden Anlage und steht hoechstens auf {@code DEBUG}; in der Antwort steht die
 * Einordnung.
 *
 * @param serviceId die Kennung aus {@code ServiceDefaultFileStore}
 * @param zustand das Ergebnis der Pruefung dieses einen Ziels
 */
public record Zielstand(String serviceId, Ablagenzustand zustand) {}
