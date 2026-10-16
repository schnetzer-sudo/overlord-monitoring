package de.kraftwerkone.overlord.monitor.assistent;

import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die Endpunkte des Assistenten ({@code docs/assistent.md} §6).
 *
 * <p><b>Kein Endpunkt nimmt eine Mandanten-ID entgegen</b> (Regel M1); es entsteht keine neue
 * Ausnahme. Der Mandant kommt aus der Sitzung ueber {@code MandantService.aktuellerKontext}. Rollen
 * {@code MANDANT} und {@code ADMIN}, jeweils fuer den aktiven Mandanten; der Aenderungszwang beim
 * Passwort sperrt wie ueberall.
 *
 * <p><b>Ein Gespraech gehoert Sitzung, Nutzer und Mandant.</b> Ein fremdes, abgelaufenes oder
 * unbekanntes ist {@code 404}, in allen Faellen gleich.
 */
@RestController
public class AssistentController {

  private final AssistentService assistentService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  AssistentController(
      AssistentService assistentService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung) {
    this.assistentService = assistentService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  /** Ob der Assistent verfuegbar ist und mit welchen Typen. */
  @GetMapping("/api/assistent")
  public AssistentResponse stand(HttpServletRequest request) {
    return assistentService.stand(mandant(), sitzung(request));
  }

  /** Eroeffnet ein Gespraech. Nicht verfuegbar: {@code 503}. */
  @PostMapping("/api/assistent/gespraeche")
  @ResponseStatus(HttpStatus.CREATED)
  public GespraechResponse eroeffnen(HttpServletRequest request) {
    return assistentService.eroeffne(erforderlicherNutzer(), mandant(), sitzung(request));
  }

  /** Beantwortet eine Nachricht des Kunden; entsteht ein gueltiger Vorschlag, mit Karte. */
  @PostMapping("/api/assistent/gespraeche/{id}/nachrichten")
  public NachrichtResponse nachricht(
      @PathVariable String id, @RequestBody NachrichtRequest anfrage, HttpServletRequest request) {
    return assistentService.sende(
        erforderlicherNutzer(), mandant(), sitzung(request), id, anfrage.text());
  }

  /** Beendet ein Gespraech. */
  @DeleteMapping("/api/assistent/gespraeche/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void beenden(@PathVariable String id, HttpServletRequest request) {
    assistentService.beende(erforderlicherNutzer(), mandant(), sitzung(request), id);
  }

  private MandantContext mandant() {
    return mandantService.aktuellerKontext(erforderlicherNutzer());
  }

  private AngemeldeterNutzer erforderlicherNutzer() {
    return sitzungsVerwaltung
        .aktuellerNutzer()
        .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
  }

  /** Die Sitzung, an die die Gespraeche gebunden sind. Ihre Kennung geht nirgends hin. */
  private static String sitzung(HttpServletRequest request) {
    HttpSession sitzung = request.getSession(false);
    if (sitzung == null) {
      throw new IllegalStateException("Angemeldeter Endpunkt ohne Sitzung erreicht");
    }
    return sitzung.getId();
  }
}
