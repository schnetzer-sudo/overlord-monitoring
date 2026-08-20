package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Der Prozess-Katalog: die Flaeche, auf der aus etwas Lesbarem etwas Gruppierbares wird.
 *
 * <p><b>Kein Endpunkt nimmt eine Mandanten-ID entgegen</b> (Regel M1). Der Mandant kommt aus der
 * Sitzung ueber {@code MandantService.aktuellerKontext}, der den Sitzungswert gegen die zulaessige
 * Menge prueft, statt ihm zu glauben. Der Katalog erbt M1 bis M4 unveraendert ({@code
 * docs/prozess-katalog.md} §7) — auch der Heuristik-Knopf laeuft fuer den <b>aktiven</b> Mandanten
 * und nicht fuer alle.
 *
 * <p><b>Die Rollengrenze steht in {@code SecurityConfig}</b> und nicht als Annotation hier —
 * dieselbe Entscheidung wie bei {@code AdminUserController}: Die Regel soll an einer Stelle stehen.
 * {@code /api/katalog/**} verlangt die Rolle {@code ADMIN}; die Katalogpflege gehoert laut {@code
 * IMPLEMENTIERUNGSPLAN_MVP.md} (Schritt 9b) in den Administrationsbereich.
 *
 * <p>Pfade deutsch, Antwortfelder camelCase, Fehlertexte deutsch — wie im gesamten Projekt.
 */
@RestController
public class ProzessKatalogController {

  private final ProzessKatalogService katalogService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  ProzessKatalogController(
      ProzessKatalogService katalogService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung) {
    this.katalogService = katalogService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  /** Was gesetzt werden soll. Beide Felder duerfen fehlen — leer ist ein gueltiger Wert (E4). */
  public record ZuordnenRequest(String partner, String richtung) {}

  /** Eine Massenzuordnung: <b>ein</b> Feld, ein Wert, ein Projekt, ein Modus (E11). */
  public record MassenzuordnungRequest(String projectId, String feld, String wert, String modus) {}

  /**
   * Die Pflegeliste des aktiven Mandanten.
   *
   * <p><b>Keine Paginierung</b> (E8): 733 Prozesse beim groessten Mandanten kosten in der Datenbank
   * rund vier Millisekunden (L12). Die Fortschrittszahl bekommt <b>keine eigene Abfrage</b> — die
   * volle Liste kommt zurueck und wird vorne gezaehlt.
   *
   * @param nurOffene der Arbeitsmodus, nicht die Bequemlichkeit (E7). Vorgabe {@code false}
   */
  @GetMapping("/api/katalog/prozesse")
  public List<KatalogzeileResponse> prozesse(
      @RequestParam(defaultValue = "false") boolean nurOffene) {
    return katalogService.pflegeliste(mandant(), nurOffene);
  }

  /**
   * Die Partner-Auswahlliste, abgeleitet aus den Katalogzeilen des aktiven Mandanten — <b>keine
   * Tabelle {@code partner}</b> (E2).
   */
  @GetMapping("/api/katalog/partner")
  public List<String> partner() {
    return katalogService.partner(mandant());
  }

  /**
   * Setzt Partner und Richtung eines Prozesses und macht ihn gepflegt.
   *
   * <p>Eine {@code processId} ausserhalb des aktiven Mandanten ergibt {@code 404}, nicht {@code
   * 403}.
   */
  @PutMapping("/api/katalog/prozesse/{processId}")
  public KatalogzeileResponse zuordnen(
      @PathVariable String processId,
      @RequestBody ZuordnenRequest anfrage,
      HttpServletRequest request) {
    return katalogService.zuordnen(
        erforderlicherNutzer(),
        mandant(),
        processId,
        anfrage.partner(),
        anfrage.richtung(),
        request.getRemoteAddr());
  }

  /**
   * Setzt ein Feld fuer alle Prozesse eines Projekts — in der Vorschau oder wirklich.
   *
   * <p>Ein Projekt ausserhalb des aktiven Mandanten ergibt {@code 404}.
   */
  @PostMapping("/api/katalog/massenzuordnung")
  public MassenzuordnungResponse massenzuordnung(
      @RequestBody MassenzuordnungRequest anfrage, HttpServletRequest request) {
    return katalogService.massenzuordnung(
        erforderlicherNutzer(),
        mandant(),
        anfrage.projectId(),
        anfrage.feld(),
        anfrage.wert(),
        anfrage.modus(),
        request.getRemoteAddr());
  }

  /**
   * Laesst die Heuristik ueber den aktiven Mandanten laufen (E13).
   *
   * <p>Sie legt fehlende Zeilen an, frischt offene auf und ruehrt gepflegte nie an. <b>Ein zweiter
   * Lauf aendert an gepflegten Zeilen nichts</b> — das ist das Abnahmekriterium.
   */
  @PostMapping("/api/katalog/vorschlagen")
  public VorschlagslaufResponse vorschlagen(HttpServletRequest request) {
    return katalogService.vorschlagen(erforderlicherNutzer(), mandant(), request.getRemoteAddr());
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
