/**
 * Anmeldung, serverseitige Session, Anmeldesperre und das Herstellen des {@code MandantContext}
 * ({@code MandantContextProvider}, {@code MandantService}), der als erster Pflichtparameter in jede
 * Repository-Methode geht. Der Typ selbst liegt seit dem 17.09.2026 in {@code common} ({@code
 * docs/live-rest.md} §7).
 */
package de.kraftwerkone.overlord.monitor.security;
