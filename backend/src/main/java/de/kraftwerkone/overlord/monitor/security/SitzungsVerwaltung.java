package de.kraftwerkone.overlord.monitor.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Alles, was die serverseitige Sitzung angeht: anlegen, erneuern, beenden.
 *
 * <p><b>Die Sitzungs-ID wird bei jeder Rechteaenderung erneuert</b> — beim Anmelden und nach der
 * Passwortaenderung. Ohne das bliebe eine vor der Anmeldung untergeschobene ID gueltig
 * (Sitzungsfestschreibung). Verwendet wird dafuer die Standardstrategie von Spring Security, nicht
 * eine eigene: {@code ChangeSessionIdAuthenticationStrategy} behaelt die Attribute und tauscht nur
 * die ID.
 *
 * <p>Reihenfolge beim Anmelden, bewusst so und nicht anders:
 *
 * <ol>
 *   <li>Sitzungs-ID erneuern — <b>bevor</b> irgendetwas Angemeldetes in der Sitzung landet.
 *   <li>{@code SecurityContext} setzen und speichern (legt die Sitzung an, falls es noch keine
 *       gibt).
 *   <li>Erst danach den aktiven Mandanten setzen.
 * </ol>
 *
 * <p>Die Sitzung selbst liegt in {@code overlord_monitor.SPRING_SESSION} (Spring Session JDBC),
 * nicht im Arbeitsspeicher: Sie muss sich jederzeit serverseitig zuruecknehmen lassen — das ist der
 * Grund, aus dem dieses Projekt kein JWT verwendet.
 */
@Component
public class SitzungsVerwaltung {

  private final SecurityContextRepository securityContextRepository;
  private final SessionAuthenticationStrategy sitzungsStrategie;
  private final MandantContextProvider mandantContextProvider;
  private final SecurityContextHolderStrategy kontextHalter =
      SecurityContextHolder.getContextHolderStrategy();

  SitzungsVerwaltung(
      SecurityContextRepository securityContextRepository,
      SessionAuthenticationStrategy sitzungsStrategie,
      MandantContextProvider mandantContextProvider) {
    this.securityContextRepository = securityContextRepository;
    this.sitzungsStrategie = sitzungsStrategie;
    this.mandantContextProvider = mandantContextProvider;
  }

  /** Meldet den Nutzer an und setzt den aktiven Mandanten, sofern einer feststeht. */
  public void starte(
      HttpServletRequest request,
      HttpServletResponse response,
      AngemeldeterNutzer nutzer,
      String mandantId) {
    schreibeKontext(request, response, nutzer);
    if (mandantId != null) {
      mandantContextProvider.setze(mandantId);
    }
  }

  /**
   * Erneuert die Sitzung nach einer Passwortaenderung und ersetzt das Principal (der
   * Aenderungszwang ist weg). Der aktive Mandant bleibt erhalten — er haengt an der Sitzung, nicht
   * an der Anmeldung.
   */
  public void erneuere(
      HttpServletRequest request, HttpServletResponse response, AngemeldeterNutzer nutzer) {
    schreibeKontext(request, response, nutzer);
  }

  /** Verwirft die Sitzung serverseitig und leert den Kontext. */
  public void beende(HttpServletRequest request, HttpServletResponse response) {
    Authentication authentication = kontextHalter.getContext().getAuthentication();
    // Invalidiert die Sitzung; Spring Session laesst daraufhin das Sitzungs-Cookie ablaufen.
    new SecurityContextLogoutHandler().logout(request, response, authentication);
  }

  /** Der angemeldete Nutzer, sofern die Anfrage einen traegt. */
  public Optional<AngemeldeterNutzer> aktuellerNutzer() {
    Authentication authentication = kontextHalter.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof AngemeldeterNutzer n)) {
      return Optional.empty();
    }
    return Optional.of(n);
  }

  private void schreibeKontext(
      HttpServletRequest request, HttpServletResponse response, AngemeldeterNutzer nutzer) {
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(nutzer, null, nutzer.authorities());
    // Erst die ID tauschen, dann speichern.
    sitzungsStrategie.onAuthentication(authentication, request, response);
    SecurityContext kontext = kontextHalter.createEmptyContext();
    kontext.setAuthentication(authentication);
    kontextHalter.setContext(kontext);
    securityContextRepository.saveContext(kontext, request, response);
  }
}
