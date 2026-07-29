package de.kraftwerkone.overlord.monitor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sorgt dafuer, dass das CSRF-Cookie tatsaechlich beim Aufrufer ankommt.
 *
 * <p>Spring Security erzeugt den CSRF-Token seit Version 6 <b>verzoegert</b>: Er entsteht erst,
 * wenn jemand ihn liest. Bei einer reinen JSON-API liest ihn niemand — das Cookie wuerde nie
 * geschrieben, und die erste schreibende Anfrage scheiterte mit {@code 403}. Ein einziger
 * Lesezugriff je Anfrage loest das.
 *
 * <p>Der Filter haengt <b>vor</b> der Autorisierung, damit das Cookie auch dann gesetzt wird, wenn
 * die Anfrage anschliessend mit {@code 401} endet. Genau das braucht der Anmeldevorgang: Ein
 * unangemeldeter Aufruf holt sich damit das Token, bevor er {@code POST /api/auth/login} schickt.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    if (token != null) {
      // Der Aufruf selbst ist der Zweck: er materialisiert den Token und loest das Schreiben aus.
      token.getToken();
    }
    filterChain.doFilter(request, response);
  }
}
