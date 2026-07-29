package de.kraftwerkone.overlord.monitor.config;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.security.CsrfCookieFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Spring Security 7. Konfiguriert wird ueber einen {@link SecurityFilterChain}-Bean und die
 * Lambda-DSL — {@code WebSecurityConfigurerAdapter} und {@code authorizeRequests()} stammen aus
 * aelteren Generationen und existieren nicht mehr.
 *
 * <p>Kein {@code formLogin}, kein {@code httpBasic}, kein Logout-Filter: Anmeldung, Abmeldung und
 * Passwortwechsel sind eigene Endpunkte mit eigener Auskunftsdisziplin (siehe {@code
 * security/AnmeldeService}). Bliebe zusaetzlich ein Standardpfad offen, gaebe es zwei Wege hinein —
 * und nur einer waere geprueft.
 *
 * <p><b>Fehler gehen durch den {@code HandlerExceptionResolver}.</b> Damit uebersetzt weiterhin
 * genau eine Stelle Ausnahmen in Antworten (der {@code @RestControllerAdvice} in {@code common}),
 * auch fuer die beiden Faelle, die Spring Security ausserhalb des DispatcherServlets abfaengt.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Ob Cookies das Attribut {@code Secure} tragen. Kommt aus {@code
   * server.servlet.session.cookie.secure} und ist damit dieselbe Quelle wie fuer das
   * Sitzungs-Cookie: im Profil {@code dev} aus (sonst funktioniert {@code http://localhost} nicht),
   * sonst an.
   */
  private final boolean cookiesSecure;

  SecurityConfig(@Value("${server.servlet.session.cookie.secure}") boolean cookiesSecure) {
    this.cookiesSecure = cookiesSecure;
  }

  /**
   * BCrypt mit Kostenfaktor 12. Der Wert ist der Zweck: Ein Vergleich dauert damit gut zweihundert
   * Millisekunden statt weniger als eine — genau das macht das Durchprobieren teuer.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  /**
   * Dieselbe Ablage, die Spring Security auch selbst verwenden wuerde. Als Bean, damit {@code
   * SitzungsVerwaltung} beim Anmelden nachweislich dieselbe benutzt und nicht eine zweite.
   */
  @Bean
  public SecurityContextRepository securityContextRepository() {
    return new DelegatingSecurityContextRepository(
        new RequestAttributeSecurityContextRepository(),
        new HttpSessionSecurityContextRepository());
  }

  /**
   * Sitzungsfestschreibung: Beim Anmelden wird die Sitzungs-ID getauscht, die Attribute bleiben.
   * Das ist der Spring-Security-Standard und wird nicht abgeschaltet.
   */
  @Bean
  public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
    return new ChangeSessionIdAuthenticationStrategy();
  }

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      SecurityContextRepository securityContextRepository,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver fehlerUebersetzer)
      throws Exception {

    // withHttpOnlyFalse: Der BFF in Teil 2 liest das Cookie und schickt den Wert als Header
    // zurueck. Deshalb auch der einfache Request-Handler statt des XOR-Handlers — der erwartet
    // einen maskierten Wert und wiese den unveraendert zurueckgeschickten Token ab.
    CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrfRepository.setCookieCustomizer(cookie -> cookie.secure(cookiesSecure).sameSite("Lax"));

    http.csrf(
            csrf ->
                csrf.csrfTokenRepository(csrfRepository)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth
                    // Ohne diese Zeile beantwortet der Fehler-Dispatch selbst wieder mit einem
                    // Fehler und die eigentliche Ursache verschwindet.
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC)
                    .permitAll()
                    // Anmelden und Abmelden muessen ohne Sitzung erreichbar sein.
                    .requestMatchers("/api/auth/login", "/api/auth/logout")
                    .permitAll()
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .securityContext(kontext -> kontext.securityContextRepository(securityContextRepository))
        .exceptionHandling(
            fehler ->
                fehler
                    .authenticationEntryPoint(
                        (request, response, ex) ->
                            fehlerUebersetzer.resolveException(
                                request,
                                response,
                                null,
                                new FachlicheAusnahme(
                                    HttpStatus.UNAUTHORIZED,
                                    "nicht-angemeldet",
                                    "Nicht angemeldet",
                                    "Melde dich an, um fortzufahren.",
                                    "Zugriff ohne gueltige Sitzung auf "
                                        + request.getRequestURI())))
                    .accessDeniedHandler(
                        (request, response, ex) ->
                            fehlerUebersetzer.resolveException(
                                request,
                                response,
                                null,
                                new FachlicheAusnahme(
                                    HttpStatus.FORBIDDEN,
                                    "zugriff-verweigert",
                                    "Zugriff verweigert",
                                    "Dieser Bereich ist fuer deine Rolle nicht freigegeben.",
                                    "Rolle reicht nicht fuer " + request.getRequestURI()))))
        // Keine Umleitung auf eine Anmeldeseite und kein Zwischenspeichern der urspruenglichen
        // Anfrage: Diese Anwendung antwortet einem Programm, nicht einem Browser-Formular.
        .requestCache(cache -> cache.disable())
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);
    // Die anonyme Authentifizierung bleibt bewusst an: Sie ist die Art, wie Spring Security
    // „nicht angemeldet" darstellt, und nur mit ihr landet ein Zugriff ohne Sitzung verlaesslich
    // beim AuthenticationEntryPoint (401) statt beim AccessDeniedHandler (403).

    return http.build();
  }
}
