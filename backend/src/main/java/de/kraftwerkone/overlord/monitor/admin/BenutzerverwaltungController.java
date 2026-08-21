package de.kraftwerkone.overlord.monitor.admin;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Benutzerverwaltung, Teil zwei: auflisten, sperren, deaktivieren, Rolle, Mandanten, Passwort
 * zuruecksetzen (Schritt 9a). Teil eins — das Anlegen — steht unveraendert in {@link
 * AdminUserController} (E4).
 *
 * <p><b>Ausschliesslich fuer die Rolle {@code ADMIN}</b>, durchgesetzt in {@code
 * config/SecurityConfig} ueber {@code /api/admin/**} und nicht hier per Annotation, damit die Regel
 * an einer Stelle steht. Die bestehende Regel deckt die neuen Unterpfade ab — sie ist am 21.08.2026
 * geprueft und nicht ergaenzt worden; eine zweite Regel daneben waere der Fall, bei dem spaeter
 * niemand mehr weiss, welche greift.
 *
 * <p><b>Die Rollengrenze ist hier die Mandantengrenze.</b> Die Nutzerverwaltung ist mandantenfrei
 * (E2), es gibt also keine Grenze, an der ein Mandantenleck entstehen koennte. Regel M4 wird
 * deshalb zur Rollenpruefung: Jeder dieser Endpunkte gibt einem MANDANT-Nutzer {@code 403} —
 * <b>auch dann, wenn er auf dessen eigenes Konto zeigt</b> (E3). Genau dieser Fall ist der Kern,
 * denn er ist der einzige, bei dem eine Berechtigungspruefung ueber den Kontoinhaber plausibel
 * aussieht und trotzdem falsch ist.
 *
 * <p>Pfade und Antwortfelder englisch, Fehlertexte deutsch. Das Projekt fuehrt Fachpfade deutsch
 * ({@code /api/nachrichten}, {@code /dateien}) und Infrastrukturpfade englisch ({@code
 * /api/auth/...}, das bestehende {@code /api/admin/users}); deutsche Unterpfade unter einer
 * englischen Sammlung waeren schlechter als beide reinen Varianten.
 */
@RestController
@RequestMapping("/api/admin/users")
public class BenutzerverwaltungController {

  private final BenutzerverwaltungService benutzerverwaltung;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  BenutzerverwaltungController(
      BenutzerverwaltungService benutzerverwaltung, SitzungsVerwaltung sitzungsVerwaltung) {
    this.benutzerverwaltung = benutzerverwaltung;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  /** Sperren oder entsperren — ein Zustand, kein Verb im Pfad. */
  public record SperreRequest(boolean locked) {}

  /** Deaktivieren oder reaktivieren. Es gibt kein Loeschen (E8). */
  public record AktivRequest(boolean active) {}

  public record RolleRequest(@NotBlank(message = "Rolle fehlt") String role) {}

  /**
   * Die neue Mandantenmenge, vollstaendig — nicht ein Zusatz und nicht ein Entzug.
   *
   * <p><b>Dies ist die dritte und derzeit letzte Ausnahme von Regel M1.</b> Sie ist zulaessig, weil
   * hier eine Berechtigung <i>definiert</i> und kein Datenausschnitt <i>abgefragt</i> wird.
   */
  public record MandantenRequest(List<String> tenants) {}

  /** Das vom Admin getippte Einmalpasswort. Es steht in keiner Antwort und in keinem Protokoll. */
  public record PasswortRequest(
      @NotBlank(message = "Einmalpasswort fehlt") String initialPassword) {}

  /**
   * Alle Konten, mandantenfrei (E2). <b>Keine Paginierung, keine serverseitige Suche</b> (E16) —
   * Groessenordnung dreissig Konten, jede Mechanik dafuer waere Beiwerk.
   */
  @GetMapping
  public List<NutzerzeileResponse> liste() {
    return benutzerverwaltung.liste();
  }

  @PutMapping("/{id}/lock")
  public NutzerzeileResponse sperre(
      @PathVariable long id, @RequestBody SperreRequest anfrage, HttpServletRequest request) {
    return benutzerverwaltung.setzeSperre(admin(), id, anfrage.locked(), request.getRemoteAddr());
  }

  @PutMapping("/{id}/active")
  public NutzerzeileResponse aktiv(
      @PathVariable long id, @RequestBody AktivRequest anfrage, HttpServletRequest request) {
    return benutzerverwaltung.setzeAktiv(admin(), id, anfrage.active(), request.getRemoteAddr());
  }

  @PutMapping("/{id}/role")
  public NutzerzeileResponse rolle(
      @PathVariable long id, @Valid @RequestBody RolleRequest anfrage, HttpServletRequest request) {
    return benutzerverwaltung.setzeRolle(admin(), id, anfrage.role(), request.getRemoteAddr());
  }

  @PutMapping("/{id}/tenants")
  public NutzerzeileResponse mandanten(
      @PathVariable long id, @RequestBody MandantenRequest anfrage, HttpServletRequest request) {
    return benutzerverwaltung.setzeMandanten(
        admin(), id, anfrage.tenants(), request.getRemoteAddr());
  }

  @PostMapping("/{id}/password")
  public NutzerzeileResponse passwort(
      @PathVariable long id,
      @Valid @RequestBody PasswortRequest anfrage,
      HttpServletRequest request) {
    return benutzerverwaltung.setzePasswort(
        admin(), id, anfrage.initialPassword(), request.getRemoteAddr());
  }

  private AngemeldeterNutzer admin() {
    return sitzungsVerwaltung
        .aktuellerNutzer()
        .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
  }
}
