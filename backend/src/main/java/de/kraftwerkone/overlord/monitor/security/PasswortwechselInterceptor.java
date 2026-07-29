package de.kraftwerkone.overlord.monitor.security;

import de.kraftwerkone.overlord.monitor.common.error.PasswortwechselErforderlichException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Sperrt alle Endpunkte, solange ein Konto unter Aenderungszwang steht.
 *
 * <p>Erlaubt bleiben genau drei: Selbstauskunft (sonst weiss die Oberflaeche nicht, warum sie
 * blockiert), Passwortaenderung (der Ausweg) und Abmeldung. Alles andere antwortet {@code 403} mit
 * Problemtyp {@code passwortwechsel-erforderlich} — die Oberflaeche kann daran den Grund erkennen,
 * ohne den Text zu lesen.
 *
 * <p>Bewusst ein {@link HandlerInterceptor} und kein Servlet-Filter: Eine hier geworfene Ausnahme
 * laeuft durch den {@code @RestControllerAdvice} und wird dort — an der <b>einzigen</b> Stelle, die
 * Ausnahmen uebersetzt — zu {@code problem+json}. Ein Filter muesste die Antwort selbst schreiben
 * und das Format ein zweites Mal nachbauen.
 */
@Component
public class PasswortwechselInterceptor implements HandlerInterceptor {

  private static final Set<String> ERLAUBTE_PFADE =
      Set.of("/api/auth/login", "/api/auth/logout", "/api/auth/me", "/api/auth/password");

  private final SitzungsVerwaltung sitzungsVerwaltung;

  PasswortwechselInterceptor(SitzungsVerwaltung sitzungsVerwaltung) {
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String pfad = request.getRequestURI().substring(request.getContextPath().length());
    if (ERLAUBTE_PFADE.contains(pfad)) {
      return true;
    }
    sitzungsVerwaltung
        .aktuellerNutzer()
        .filter(AngemeldeterNutzer::mustChangePassword)
        .ifPresent(
            nutzer -> {
              throw new PasswortwechselErforderlichException(
                  "Zugriff auf " + pfad + " bei gesetztem Aenderungszwang, Nutzer " + nutzer.id());
            });
    return true;
  }
}
