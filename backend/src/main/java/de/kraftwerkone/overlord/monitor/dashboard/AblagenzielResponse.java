package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Ein einzelnes Ziel der Ablagenkachel.
 *
 * <p><b>Es steht in der Antwort, obwohl die Kachel zusammenfasst</b> — und das ist der Unterschied
 * zwischen einer Ampel und einer Auskunft: „nicht erreichbar" ohne Kennung liesse offen, welche
 * Ablage gemeint ist, und in der Produktion sind es in der Regel zwei.
 *
 * @param serviceId die Kennung aus {@code ServiceDefaultFileStore}
 * @param zustand das Ergebnis der letzten Pruefung dieses Ziels. <b>Keine Adresse, keine
 *     Rohantwort</b> (Regel G1)
 */
public record AblagenzielResponse(String serviceId, Ablagenzustand zustand) {}
