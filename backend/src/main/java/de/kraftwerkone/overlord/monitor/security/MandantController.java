package de.kraftwerkone.overlord.monitor.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die waehlbaren Mandanten und der Wechsel zwischen ihnen.
 *
 * <p>{@code POST /api/auth/mandant} ist <b>eine der genau zwei Stellen im System, die eine
 * Mandanten-ID entgegennehmen</b> (die andere ist {@code POST /api/admin/users}). Beide sind
 * namentlich in {@code docs/mandantentrennung.md} gefuehrt; taucht dort jemals eine dritte auf, ist
 * das ein Signal und keine Kleinigkeit.
 *
 * <p>Die Ausnahme ist vertretbar, weil die ID hier <b>nicht</b> bestimmt, was gelesen werden darf,
 * sondern ausschliesslich aus der ohnehin zulaessigen Menge auswaehlt. Liegt sie nicht darin, ist
 * die Antwort {@code 404} — dieselbe wie fuer eine erfundene ID.
 */
@RestController
public class MandantController {

  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  MandantController(MandantService mandantService, SitzungsVerwaltung sitzungsVerwaltung) {
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  public record WechselRequest(@NotBlank(message = "Mandant fehlt") String mandantId) {}

  /**
   * Genau die Menge, aus der dieser Nutzer waehlen darf: fuer ADMIN alle Mandanten, fuer MANDANT
   * die eigenen. Nichts darueber hinaus — die Liste ist selbst eine Auskunft.
   */
  @GetMapping("/api/mandanten")
  public List<MandantResponse> waehlbare() {
    return mandantService.zulaessige(erforderlicherNutzer());
  }

  /** Wechselt den aktiven Mandanten und liefert die aktualisierte Selbstauskunft. */
  @PostMapping("/api/auth/mandant")
  public SelbstauskunftResponse wechseln(
      @Valid @RequestBody WechselRequest anfrage, HttpServletRequest request) {
    AngemeldeterNutzer nutzer = erforderlicherNutzer();
    MandantResponse ziel =
        mandantService.wechsle(nutzer, anfrage.mandantId(), request.getRemoteAddr());
    return new SelbstauskunftResponse(
        nutzer.username(),
        nutzer.rolle().name(),
        ziel,
        nutzer.mustChangePassword(),
        nutzer.downloadAllowed());
  }

  private AngemeldeterNutzer erforderlicherNutzer() {
    return sitzungsVerwaltung
        .aktuellerNutzer()
        .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
  }
}
