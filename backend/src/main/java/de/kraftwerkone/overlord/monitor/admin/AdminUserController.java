package de.kraftwerkone.overlord.monitor.admin;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Benutzerverwaltung, Teil eins: Anlegen. Ausschliesslich fuer die Rolle {@code ADMIN} —
 * durchgesetzt in {@code config/SecurityConfig} ueber {@code /api/admin/**}, nicht hier per
 * Annotation, damit die Regel an einer Stelle steht.
 *
 * <p>Dieser Endpunkt nimmt eine Mandanten-ID entgegen und ist damit die <b>zweite und letzte</b>
 * Ausnahme von Regel M1. Beide Ausnahmen sind namentlich in {@code docs/mandantentrennung.md}
 * gefuehrt.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

  private final AdminUserService adminUserService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  AdminUserController(AdminUserService adminUserService, SitzungsVerwaltung sitzungsVerwaltung) {
    this.adminUserService = adminUserService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  /**
   * @param initialPassword Einmalpasswort. Das angelegte Konto startet mit Aenderungszwang; dieses
   *     Passwort taugt genau fuer die erste Anmeldung.
   */
  public record AnlegenRequest(
      @NotBlank(message = "Benutzername fehlt") String username,
      @NotBlank(message = "Rolle fehlt") String role,
      @NotBlank(message = "Mandant fehlt") String mandantId,
      @NotBlank(message = "Einmalpasswort fehlt") String initialPassword) {}

  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  public AngelegterNutzerResponse anlegen(
      @Valid @RequestBody AnlegenRequest anfrage, HttpServletRequest request) {
    AngemeldeterNutzer admin =
        sitzungsVerwaltung
            .aktuellerNutzer()
            .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
    return adminUserService.legeAn(
        admin,
        anfrage.username(),
        anfrage.role(),
        anfrage.mandantId(),
        anfrage.initialPassword(),
        request.getRemoteAddr());
  }
}
