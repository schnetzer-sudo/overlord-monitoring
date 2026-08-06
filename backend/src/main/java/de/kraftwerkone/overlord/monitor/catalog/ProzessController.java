package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die Prozesse, aus denen der Nutzer seinen Filter waehlt.
 *
 * <p><b>Er nimmt keine Mandanten-ID entgegen</b> (Regel M1) und gehoert ausdruecklich <b>nicht</b>
 * zu den zwei namentlich gefuehrten Ausnahmen in {@code docs/mandantentrennung.md} §3. Der Mandant
 * kommt aus der Sitzung ueber {@code MandantService.aktuellerKontext}, der den Sitzungswert gegen
 * die zulaessige Menge prueft, statt ihm zu glauben.
 *
 * <p><b>Warum dieser Endpunkt ueberhaupt existiert.</b> {@code /api/nachrichten} nimmt {@code
 * prozess} mehrfach entgegen — aber die Menge, aus der zu waehlen ist, stand nirgends. Ohne sie
 * muesste die Oberflaeche die Prozesse aus den gerade angezeigten Zeilen zusammensammeln, und der
 * Filter zeigte dann nur, was ohnehin schon zu sehen ist. Genau der Prozess, der <b>nichts</b>
 * geliefert hat, fehlte in der Auswahl.
 *
 * <p><b>Warum ohne Service.</b> Zwischen HTTP und SQL ist hier nichts zu entscheiden: kein
 * Zeitfenster aufzuloesen, kein Suchbegriff vorzufiltern, keine Rohwerte einzuordnen. Eine
 * Service-Klasse waere eine Weiterleitung mit eigenem Dateinamen. Kommt spaeter Fachlogik dazu —
 * etwa die Anreicherung um den kuratierten Partner aus {@code process_catalog} in Schritt 9 —,
 * entsteht sie an dieser Stelle.
 */
@RestController
public class ProzessController {

  private final ProzessRepository prozessRepository;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  ProzessController(
      ProzessRepository prozessRepository,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung) {
    this.prozessRepository = prozessRepository;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  /**
   * Alle Prozesse des aktiven Mandanten, nach Projekt und Name sortiert.
   *
   * <p><b>Keine Paginierung, kein Zeitfenster</b> — und das ist keine Nachlaessigkeit gegenueber
   * den Regeln L1 und L3, sondern ihre Anwendung: Beide gelten fuer {@code Message}. Hier stehen
   * 1.490 Zeilen ueber <b>alle</b> Mandanten (Messung M1), fuer einen einzelnen ein Bruchteil
   * davon. Ein Cursor auf einer Auswahlliste, die vollstaendig in ein Auswahlfeld passt, waere
   * Aufwand ohne Gegenwert; ein Zeitfenster wuerde ausgerechnet die stillen Prozesse ausblenden.
   */
  @GetMapping("/api/prozesse")
  public List<ProzessResponse> prozesse() {
    MandantContext mandant = mandantService.aktuellerKontext(erforderlicherNutzer());
    return prozessRepository.findeAlle(mandant);
  }

  private AngemeldeterNutzer erforderlicherNutzer() {
    return sitzungsVerwaltung
        .aktuellerNutzer()
        .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
  }
}
