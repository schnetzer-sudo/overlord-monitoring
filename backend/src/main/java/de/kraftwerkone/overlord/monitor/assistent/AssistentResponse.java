package de.kraftwerkone.overlord.monitor.assistent;

import java.util.List;

/**
 * Die Antwort von {@code GET /api/assistent} ({@code docs/assistent.md} §6.1).
 *
 * @param verfuegbar Schalter an, Schluessel gesetzt und mindestens ein Typ angeboten
 * @param typen die Namen der angebotenen Typen, nach Kennung sortiert
 */
public record AssistentResponse(boolean verfuegbar, List<String> typen) {}
