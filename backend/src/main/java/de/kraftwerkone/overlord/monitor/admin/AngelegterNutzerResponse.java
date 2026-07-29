package de.kraftwerkone.overlord.monitor.admin;

/**
 * Antwort auf {@code POST /api/admin/users}.
 *
 * <p><b>Enthaelt niemals das Passwort</b> — auch nicht das eben vergebene Einmalpasswort. Es geht
 * ausschliesslich auf dem Weg zum Nutzer, den der Admin selbst waehlt, und steht in keiner Antwort,
 * keinem Protokoll und keinem Audit-Eintrag.
 */
public record AngelegterNutzerResponse(long id, String username, String role, String mandantId) {}
