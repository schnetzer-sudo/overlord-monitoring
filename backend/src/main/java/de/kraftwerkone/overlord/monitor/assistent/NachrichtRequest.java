package de.kraftwerkone.overlord.monitor.assistent;

/**
 * Der Rumpf von {@code POST /api/assistent/gespraeche/{id}/nachrichten}: was der Kunde schreibt.
 * Ein untergeschobenes {@code mandantId} faellt beim Lesen weg (Regel M1).
 */
public record NachrichtRequest(String text) {}
