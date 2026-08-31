package de.kraftwerkone.overlord.monitor.dashboard;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die Landingpage: <b>ein Endpunkt, ein Aufruf, eine Antwort</b>.
 *
 * <h2>Kein Mandantenparameter — in keiner Form</h2>
 *
 * <p>Regel M1. Der Mandant kommt aus der Sitzung, ueber {@code SitzungsVerwaltung} und {@code
 * MandantService}; ein {@code ?mandant=…} ist hier kein Fehler, sondern schlicht wirkungslos, und
 * genau das prueft {@code DashboardIsolationDbIT}. <b>Dieser Endpunkt ist keine neue benannte
 * Ausnahme</b> — die drei, die es gibt, stehen in {@code docs/mandantentrennung.md} §3 und
 * definieren allesamt eine Berechtigung, statt einen Datenausschnitt abzufragen.
 *
 * <h2>Ein Parameter, und er ist freiwillig</h2>
 *
 * <p>{@code zeitraum} waehlt eines der drei Paare ({@link Dashboardzeitraum}). Fehlt er, waehlt der
 * Endpunkt selbst und <b>nennt das gewaehlte Paar in der Antwort</b> — sonst wuesste die
 * Oberflaeche nicht, was sie hervorheben und in die URL schreiben soll.
 *
 * <h2>Kein Rolleneintrag in {@code SecurityConfig}, und das ist richtig</h2>
 *
 * <p>Der Pfad faellt unter {@code anyRequest().authenticated()}. Eingetragen wird dort nur, wer
 * eine <b>Rollengrenze</b> braucht — der Katalog etwa, weil er {@code ADMIN} verlangt. Das
 * Dashboard zeigt jedem angemeldeten Nutzer den Ausschnitt seines aktiven Mandanten und keinen
 * anderen.
 */
@RestController
public class DashboardController {

  private final DashboardService dashboardService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  DashboardController(
      DashboardService dashboardService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung) {
    this.dashboardService = dashboardService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  @GetMapping("/api/dashboard")
  public DashboardResponse landingpage(@RequestParam(required = false) String zeitraum) {
    MandantContext mandant = mandantService.aktuellerKontext(erforderlicherNutzer());
    return dashboardService.landingpage(mandant, Dashboardzeitraum.ausCode(zeitraum));
  }

  private AngemeldeterNutzer erforderlicherNutzer() {
    return sitzungsVerwaltung
        .aktuellerNutzer()
        .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
  }
}
