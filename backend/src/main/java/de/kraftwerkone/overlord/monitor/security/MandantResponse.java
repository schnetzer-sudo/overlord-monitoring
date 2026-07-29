package de.kraftwerkone.overlord.monitor.security;

/**
 * Ein waehlbarer Mandant: lesbarer Code und Anzeigename aus {@code GlassfishDB.Mandant}.
 *
 * <p>Die {@code MandantID} ist ein sprechender Code (etwa {@code NEXANS}), keine UUID. Der Name
 * darf in der Quelle {@code null} sein; dann steht hier der Code, damit die Auswahl nie leer
 * aussieht.
 */
public record MandantResponse(String id, String name) {}
