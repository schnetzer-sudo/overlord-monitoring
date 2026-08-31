package de.kraftwerkone.overlord.monitor.dashboard;

import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;

/**
 * Wie viele Nachrichten eines Eimers in eine Einordnung fallen.
 *
 * <p>Die Einordnung kommt aus {@code MessageStatusClassifier.einordnung(rohwert)} — <b>gerufen,
 * nicht nachgebaut</b> ({@code PROJEKTBESCHREIBUNG.md} §4.1). Ein unbekannter Rohwert landet damit
 * in {@link MessageStatusKind#UNGEKLAERT} und niemals in einem geratenen Eimer (Regel Q4).
 *
 * @param einordnung die Kategorie, nicht der Rohwert — der Rohwert steht bei den Fehlerarten
 *     ({@link FehlerartResponse}), wo er gebraucht wird
 * @param anzahl die Summe der {@code anzahl}-Spalten aller Rollupzeilen dieses Eimers mit dieser
 *     Einordnung
 */
public record EinordnungszahlResponse(MessageStatusKind einordnung, long anzahl) {}
