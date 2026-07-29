package de.kraftwerkone.overlord.monitor.security;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Anmelden, Abmelden, Selbstauskunft, Passwortaenderung.
 *
 * <p><b>Kein Endpunkt hier nimmt eine Mandanten-ID entgegen</b> (Regel M1). Der Wechsel und die
 * Liste der waehlbaren Mandanten liegen in {@link MandantController} — dort steht auch die
 * Begruendung, warum genau ein Endpunkt eine ID annehmen darf.
 *
 * <p>Die Anmeldung laeuft bewusst ueber einen Controller und nicht ueber {@code formLogin}: Die
 * Reihenfolge der Pruefungen (Passwort vor Kontozustand) und die Auskunftsdisziplin stehen in
 * {@link AnmeldeService} und waeren in einem {@code AuthenticationProvider} nicht so fuehrbar. Die
 * Sitzung selbst wird ueber die Standardbausteine von Spring Security aufgebaut, siehe {@link
 * SitzungsVerwaltung}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AnmeldeService anmeldeService;
  private final PasswortService passwortService;
  private final SitzungsVerwaltung sitzungsVerwaltung;
  private final MandantService mandantService;
  private final AuditLogWriter auditLogWriter;

  AuthController(
      AnmeldeService anmeldeService,
      PasswortService passwortService,
      SitzungsVerwaltung sitzungsVerwaltung,
      MandantService mandantService,
      AuditLogWriter auditLogWriter) {
    this.anmeldeService = anmeldeService;
    this.passwortService = passwortService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
    this.mandantService = mandantService;
    this.auditLogWriter = auditLogWriter;
  }

  public record AnmeldungRequest(
      @NotBlank(message = "Benutzername fehlt") String username,
      @NotBlank(message = "Passwort fehlt") String password) {}

  public record PasswortRequest(
      @NotBlank(message = "Bisheriges Passwort fehlt") String oldPassword,
      @NotBlank(message = "Neues Passwort fehlt") String newPassword) {}

  /**
   * Meldet an. Die Antwort ist dieselbe Selbstauskunft wie {@code GET /api/auth/me} — der Aufrufer
   * soll nach dem Anmelden nichts nachschlagen muessen.
   */
  @PostMapping("/login")
  public SelbstauskunftResponse anmelden(
      @Valid @RequestBody AnmeldungRequest anfrage,
      HttpServletRequest request,
      HttpServletResponse response) {
    AnmeldeService.Ergebnis ergebnis =
        anmeldeService.anmelden(anfrage.username(), anfrage.password(), request.getRemoteAddr());
    sitzungsVerwaltung.starte(request, response, ergebnis.nutzer(), ergebnis.aktiverMandant());
    return selbstauskunft(ergebnis.nutzer());
  }

  /**
   * Verwirft die Sitzung serverseitig. Bewusst auch fuer nicht Angemeldete erreichbar: Eine
   * abgelaufene Sitzung soll sich abmelden lassen, ohne dass der Aufrufer erst ein {@code 401}
   * behandeln muss.
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> abmelden(HttpServletRequest request, HttpServletResponse response) {
    sitzungsVerwaltung
        .aktuellerNutzer()
        .ifPresent(
            nutzer ->
                auditLogWriter.schreibe(
                    new AuditEvent(
                        AuditEventType.ABMELDUNG,
                        nutzer.id(),
                        nutzer.username(),
                        null,
                        null,
                        null,
                        request.getRemoteAddr(),
                        null)));
    sitzungsVerwaltung.beende(request, response);
    return ResponseEntity.noContent().build();
  }

  /** Wer bin ich, welche Rolle, welcher Mandant ist aktiv, muss ich das Passwort wechseln. */
  @org.springframework.web.bind.annotation.GetMapping("/me")
  public SelbstauskunftResponse selbstauskunft() {
    return selbstauskunft(erforderlicherNutzer());
  }

  /**
   * Setzt ein neues Passwort. Danach ist der Aenderungszwang weg und die Sitzungs-ID erneuert —
   * eine Rechteaenderung bekommt eine neue ID.
   */
  @PostMapping("/password")
  public SelbstauskunftResponse passwortAendern(
      @Valid @RequestBody PasswortRequest anfrage,
      HttpServletRequest request,
      HttpServletResponse response) {
    AngemeldeterNutzer neu =
        passwortService.aendere(
            erforderlicherNutzer(),
            anfrage.oldPassword(),
            anfrage.newPassword(),
            request.getRemoteAddr());
    sitzungsVerwaltung.erneuere(request, response, neu);
    return selbstauskunft(neu);
  }

  private AngemeldeterNutzer erforderlicherNutzer() {
    return sitzungsVerwaltung
        .aktuellerNutzer()
        .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
  }

  private SelbstauskunftResponse selbstauskunft(AngemeldeterNutzer nutzer) {
    return new SelbstauskunftResponse(
        nutzer.username(),
        nutzer.rolle().name(),
        mandantService.aktiver(nutzer).orElse(null),
        nutzer.mustChangePassword(),
        nutzer.downloadAllowed());
  }
}
