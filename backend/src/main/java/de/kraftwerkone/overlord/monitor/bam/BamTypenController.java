package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die Belegarten zur Auswahl neben dem Suchfeld — <b>welche Nummer könnte das sein</b>.
 *
 * <p><b>Er nimmt keine Mandanten-ID entgegen</b> (Regel M1). Der Mandant kommt aus der Sitzung,
 * über {@code MandantService.aktuellerKontext}, der den Sitzungswert gegen die zulässige Menge
 * prüft, statt ihm zu glauben. Die Ausnahmeliste in {@code docs/mandantentrennung.md} §3 bleibt bei
 * zwei Einträgen und wächst hier nicht.
 *
 * <p><b>Warum ein eigener Controller neben {@code BamSucheController}.</b> Dieselbe Begründung wie
 * bei Liste, Detail, Kette und Belegdaten: Er beantwortet eine eigene Frage und hat mit der Suche
 * nichts gemeinsam außer dem Pfadpräfix. Vor allem trägt er <b>keinen</b> der Parameter, die den
 * Suchendpunkt ausmachen — kein Begriff, kein Zeitfenster, kein Limit, kein Abbruchpfad.
 *
 * <p><b>Ohne Parameter, und deshalb ohne Fehlerfall außer den beiden gemeinsamen:</b> {@code 401}
 * ohne Sitzung und {@code 403} {@code kein-mandant-gewaehlt} ohne aktiven Mandanten. Es entsteht
 * <b>kein neuer Problemtyp</b>.
 */
@RestController
public class BamTypenController {

  private final BamTypenService bamTypenService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  BamTypenController(
      BamTypenService bamTypenService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung) {
    this.bamTypenService = bamTypenService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  /**
   * Die für diesen Mandanten konfigurierten BAM-Typen, in der Reihenfolge ihres Sortierindex.
   *
   * <p><b>Eine leere Liste ist die richtige Antwort für drei Mandanten</b> ({@code EDITIONLINGERI},
   * {@code SYSTEM}, {@code WOC}) und keine Ausnahme: Sie haben keinen konfigurierten Typ (M40) und
   * suchen damit typlos. {@code 200} mit leerer Liste, kein {@code 404} — der Aufrufer stellt eine
   * Frage und benennt keine Ressource.
   */
  @GetMapping("/api/bam/typen")
  public List<BamTypResponse> typen() {
    return bamTypenService.typen(mandant());
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
