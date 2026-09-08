package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Das Angebot der Suchfläche — <b>was sich in der Kopfzeile suchen lässt</b>, aus beiden Quellen
 * (E‑99).
 *
 * <p><b>Er nimmt keine Mandanten-ID entgegen</b> (Regel M1) und überhaupt keinen Parameter. Der
 * Mandant kommt aus der Sitzung über {@code MandantService.aktuellerKontext}; die Ausnahmeliste in
 * {@code docs/mandantentrennung.md} §3 bleibt bei drei Einträgen und wächst hier nicht.
 *
 * <p><b>Warum ein eigener Endpunkt neben {@code GET /api/bam/typen}.</b> Jener liefert eine Quelle;
 * dieser liefert beide, in zwei Gruppen, weil die Oberfläche sie als zwei Gruppen zeigt. {@code
 * /api/bam/typen} bleibt unverändert bestehen — ob er später abgelöst wird, ist ein offener Punkt
 * ({@code docs/property-suche.md}) und nicht Gegenstand dieses Baus.
 *
 * <p><b>Ohne Parameter, und deshalb ohne Fehlerfall außer den beiden gemeinsamen:</b> {@code 401}
 * ohne Sitzung und {@code 403} {@code kein-mandant-gewaehlt} ohne aktiven Mandanten. Es entsteht
 * kein neuer Problemtyp.
 */
@RestController
public class SuchfelderController {

  private final SuchfelderService suchfelderService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  SuchfelderController(
      SuchfelderService suchfelderService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung) {
    this.suchfelderService = suchfelderService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  /**
   * Beide Gruppen des Angebots für den aktiven Mandanten.
   *
   * <p>Beide Listen dürfen leer sein, und beides ist eine Antwort und kein Fehler: Drei Mandanten
   * haben keinen konfigurierten BAM-Typ (M40), und für neun von zehn Mandanten enthält die
   * Feld-Gruppe nur die globalen Typ‑0‑Namen (M154). {@code 200}, kein {@code 404} — der Aufrufer
   * stellt eine Frage und benennt keine Ressource.
   */
  @GetMapping("/api/bam/suchfelder")
  public SuchfelderResponse suchfelder() {
    return suchfelderService.angebot(mandant());
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
