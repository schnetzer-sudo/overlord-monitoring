package de.kraftwerkone.overlord.monitor.assistent;

/**
 * Die Antwort auf eine Nachricht ({@code docs/assistent.md} §6.3).
 *
 * @param text die Antwort des Assistenten
 * @param vorschlag die Karte, oder {@code null}, wenn kein gueltiger Vorschlag entstanden ist
 */
public record NachrichtResponse(String text, Vorschlag vorschlag) {}
