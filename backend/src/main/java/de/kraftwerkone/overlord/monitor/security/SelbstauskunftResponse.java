package de.kraftwerkone.overlord.monitor.security;

/**
 * Antwort von {@code GET /api/auth/me} — und dieselbe Antwort nach Anmeldung, Passwortaenderung und
 * Mandantenwechsel. Der Aufrufer soll seinen Zustand nie aus mehreren Antworten zusammensetzen
 * muessen.
 *
 * @param mandant {@code null}, solange kein Mandant gewaehlt ist. Das ist bei jedem ADMIN und bei
 *     jedem Nutzer mit mehreren Mandanten der Zustand direkt nach dem Anmelden — kein Fehler,
 *     sondern eine offene Auswahl.
 * @param mustChangePassword solange {@code true}, lehnt jeder Endpunkt ausser Selbstauskunft,
 *     Passwortaenderung und Abmeldung ab
 */
public record SelbstauskunftResponse(
    String username,
    String role,
    MandantResponse mandant,
    boolean mustChangePassword,
    boolean downloadAllowed) {}
