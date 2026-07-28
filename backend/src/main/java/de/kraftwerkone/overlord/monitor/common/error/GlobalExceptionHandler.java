package de.kraftwerkone.overlord.monitor.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Die <b>einzige</b> Stelle, die Ausnahmen in Antworten uebersetzt (RFC 9457, {@code
 * application/problem+json}). Kein Controller faengt selbst.
 *
 * <p>Jede Antwort traegt eine {@code traceId} als Korrelations-ID, die auch im Serverprotokoll
 * steht. Interne Details — Stacktraces, SQL, Tabellen-, Spalten- oder Klassennamen,
 * Verbindungszeichenketten, Hostnamen — erscheinen <b>niemals</b> in der Antwort, auch nicht im
 * Dev-Profil.
 *
 * <p>Regel ab hier: eine nicht existierende Ressource und eine Ressource eines fremden Mandanten
 * liefern beide {@code 404}, niemals {@code 403} (Regel M3).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String TYP_BASIS = "https://overlord.kraftwerkone.de/probleme/";

  @ExceptionHandler(RessourceNichtGefundenException.class)
  public ProblemDetail handleNichtGefunden(
      RessourceNichtGefundenException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, "Die angeforderte Ressource wurde nicht gefunden.");
    problem.setTitle("Nicht gefunden");
    problem.setType(URI.create(TYP_BASIS + "nicht-gefunden"));
    problem.setInstance(URI.create(request.getRequestURI()));
    String traceId = mitTraceId(problem);
    log.info(
        "Nicht gefunden [traceId={}] {} {}", traceId, request.getMethod(), request.getRequestURI());
    return problem;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleTechnisch(Exception ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ein technischer Fehler ist aufgetreten. Bitte melde die angegebene Fehler-ID.");
    problem.setTitle("Technischer Fehler");
    problem.setType(URI.create(TYP_BASIS + "technischer-fehler"));
    problem.setInstance(URI.create(request.getRequestURI()));
    String traceId = mitTraceId(problem);
    // Technische Fehler: mit Stacktrace ins Protokoll, niemals in die Antwort.
    log.error(
        "Technischer Fehler [traceId={}] {} {}",
        traceId,
        request.getMethod(),
        request.getRequestURI(),
        ex);
    return problem;
  }

  /** Ergaenzt die {@code traceId} auch bei den von Spring vorbereiteten 4xx-Antworten. */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    ResponseEntity<Object> antwort =
        super.handleExceptionInternal(ex, body, headers, statusCode, request);
    if (antwort != null && antwort.getBody() instanceof ProblemDetail problem) {
      mitTraceId(problem);
    }
    return antwort;
  }

  private static String mitTraceId(ProblemDetail problem) {
    String traceId = UUID.randomUUID().toString();
    problem.setProperty("traceId", traceId);
    return traceId;
  }
}
