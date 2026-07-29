package de.kraftwerkone.overlord.monitor.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

  /**
   * Alle vorhergesehenen Fehler — gesperrtes Konto, fremder Mandant, zu grosses Zeitfenster. Ohne
   * Stacktrace protokolliert; in die Antwort geht ausschliesslich der fachliche Text.
   */
  @ExceptionHandler(FachlicheAusnahme.class)
  public ProblemDetail handleFachlich(FachlicheAusnahme ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.status(), ex.detail());
    problem.setTitle(ex.titel());
    problem.setType(URI.create(TYP_BASIS + ex.problemTyp()));
    problem.setInstance(URI.create(request.getRequestURI()));
    String traceId = mitTraceId(problem);
    // Die interne Ursache steht nur hier, nie in der Antwort.
    log.info(
        "Fachlicher Fehler [traceId={}] {} {} -> {} ({}): {}",
        traceId,
        request.getMethod(),
        request.getRequestURI(),
        ex.status().value(),
        ex.problemTyp(),
        ex.getMessage());
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

  /**
   * Feldbezogene Prueffehler kommen zusaetzlich als {@code errors: [{feld, meldung}]}. Ohne diese
   * Liste muesste die Oberflaeche raten, welches Feld gemeint ist.
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ResponseEntity<Object> antwort =
        super.handleMethodArgumentNotValid(ex, headers, status, request);
    if (antwort != null && antwort.getBody() instanceof ProblemDetail problem) {
      problem.setTitle("Eingabe unvollstaendig");
      problem.setType(URI.create(TYP_BASIS + "eingabe-ungueltig"));
      problem.setDetail("Bitte pruefe die angegebenen Felder.");
      problem.setProperty(
          "errors",
          ex.getBindingResult().getFieldErrors().stream()
              .map(fehler -> Map.of("feld", fehler.getField(), "meldung", meldung(fehler)))
              .toList());
    }
    return antwort;
  }

  private static String meldung(FieldError fehler) {
    return fehler.getDefaultMessage() == null ? "Ungueltiger Wert" : fehler.getDefaultMessage();
  }

  /**
   * Ergaenzt {@code traceId} und {@code type} bei den von Spring vorbereiteten Antworten.
   *
   * <p><b>Warum {@code type} hier noch einmal gesetzt wird.</b> Die Faelle, die {@code
   * ResponseEntityExceptionHandler} selbst behandelt — unlesbares JSON, falsche HTTP-Methode,
   * unbekannter Pfad — tragen ohne Zutun {@code about:blank}. Das ist laut RFC 9457 zulaessig, aber
   * kein Schluessel: Die Oberflaeche uebersetzt anhand des {@code type}, und mit {@code
   * about:blank} bliebe ihr nur der deutsche Text aus {@code detail}. Eine zweisprachige
   * Oberflaeche waere damit nicht sauber moeglich.
   *
   * <p>Bewusst eine grobe Zuordnung nach Statuscode und keine Liste je Ausnahmetyp: Diese Antworten
   * sind Randfaelle, die kein Nutzer im Normalbetrieb sieht. {@code 404} bekommt denselben
   * Schluessel wie {@link RessourceNichtGefundenException} — ein unbekannter Pfad und eine fremde
   * Ressource sollen sich auch hier nicht unterscheiden.
   */
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
      mitRueckfallTyp(problem);
    }
    return antwort;
  }

  /** Setzt einen stabilen Problemtyp, wo Spring keinen vergeben hat. */
  private static void mitRueckfallTyp(ProblemDetail problem) {
    URI vorhanden = problem.getType();
    if (vorhanden != null && !"about:blank".equals(vorhanden.toString())) {
      return;
    }
    int status = problem.getStatus();
    String typ;
    if (status == HttpStatus.NOT_FOUND.value()) {
      typ = "nicht-gefunden";
    } else if (status >= 500) {
      typ = "technischer-fehler";
    } else {
      typ = "anfrage-ungueltig";
    }
    problem.setType(URI.create(TYP_BASIS + typ));
  }

  private static String mitTraceId(ProblemDetail problem) {
    String traceId = UUID.randomUUID().toString();
    problem.setProperty("traceId", traceId);
    return traceId;
  }
}
