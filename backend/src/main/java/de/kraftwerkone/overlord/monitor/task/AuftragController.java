package de.kraftwerkone.overlord.monitor.task;

import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die Endpunkte der Auftraege ({@code docs/tasks.md} §6) und die Pruefung ohne Anlegen ({@code
 * docs/assistent.md} §6.5).
 *
 * <p><b>Kein Endpunkt nimmt eine Mandanten-ID entgegen</b> (Regel M1); es entsteht keine neue
 * Ausnahme. Der Mandant kommt aus der Sitzung ueber {@code MandantService.aktuellerKontext}, der
 * den Sitzungswert gegen die zulaessige Menge prueft. Rollen {@code MANDANT} und {@code ADMIN},
 * jeweils fuer den aktiven Mandanten — {@code SecurityConfig} verlangt fuer {@code /api/tasks} nur
 * eine Anmeldung, und der Aenderungszwang beim Passwort sperrt wie ueberall.
 *
 * <p><b>Pfade {@code /api/tasks}</b> und nicht deutsch (E‑1025): Tabelle und Plattformvertrag
 * heissen {@code task}. Antwortfelder deutsch, wie im ganzen Projekt.
 *
 * <p><b>Kein Zeitfenster, kein Cursor</b> (E‑1016): {@code task} ist nicht aus {@code GlassfishDB}
 * abgeleitet.
 */
@RestController
public class AuftragController {

  private final AuftragService auftragService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  AuftragController(
      AuftragService auftragService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung) {
    this.auftragService = auftragService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  /** Die dem aktiven Mandanten angebotenen Typen (E‑1020). Keiner: {@code 200} mit leerer Liste. */
  @GetMapping("/api/tasks/typen")
  public List<TypResponse> typen() {
    return auftragService.typen(mandant());
  }

  /**
   * Legt einen Auftrag an: {@code 201} mit {@code Location}. Dieselbe {@code anfrageId} im selben
   * Mandanten noch einmal: {@code 200} mit dem vorhandenen Auftrag.
   */
  @PostMapping("/api/tasks")
  public ResponseEntity<AuftragResponse> anlegen(
      @RequestBody AuftragRequest anfrage, HttpServletRequest request) {
    AuftragService.Anlage anlage =
        auftragService.lege(erforderlicherNutzer(), mandant(), anfrage, request.getRemoteAddr());
    if (!anlage.neu()) {
      return ResponseEntity.ok(anlage.auftrag());
    }
    return ResponseEntity.created(URI.create("/api/tasks/" + anlage.auftrag().taskId()))
        .body(anlage.auftrag());
  }

  /**
   * Prueft einen Auftrag wie beim Anlegen, ohne ihn anzulegen (E‑1042, {@code docs/assistent.md}
   * §6.5): {@code 200} mit der Fehlerliste, leer heisst gueltig. Nicht angebotener Typ: {@code 404}
   * wie beim Anlegen.
   */
  @PostMapping("/api/tasks/pruefung")
  public PruefungResponse pruefen(@RequestBody PruefungRequest anfrage) {
    return new PruefungResponse(auftragService.pruefe(mandant(), anfrage));
  }

  /** Die laufenden Auftraege des aktiven Mandanten und die der letzten 7 Tage, hoechstens 50. */
  @GetMapping("/api/tasks")
  public List<AuftragResponse> liste() {
    return auftragService.aktuelle(mandant());
  }

  /** Ein Auftrag. Fremde oder unbekannte Kennung: {@code 404}, ununterscheidbar. */
  @GetMapping("/api/tasks/{taskId}")
  public AuftragResponse einzeln(@PathVariable String taskId) {
    return auftragService.einzeln(mandant(), taskId);
  }

  /**
   * Loescht einen offenen Auftrag ({@code docs/tasks.md} §6.6): {@code 204}. Fremd oder unbekannt
   * {@code 404}, ununterscheidbar; nicht mehr offen {@code 409}.
   */
  @DeleteMapping("/api/tasks/{taskId}")
  public ResponseEntity<Void> loeschen(@PathVariable String taskId, HttpServletRequest request) {
    auftragService.loesche(erforderlicherNutzer(), mandant(), taskId, request.getRemoteAddr());
    return ResponseEntity.noContent().build();
  }

  private MandantContext mandant() {
    return mandantService.aktuellerKontext(erforderlicherNutzer());
  }

  private AngemeldeterNutzer erforderlicherNutzer() {
    return sitzungsVerwaltung
        .aktuellerNutzer()
        .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
  }
}
