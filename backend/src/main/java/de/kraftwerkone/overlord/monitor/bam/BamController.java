package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die Belegdaten einer Nachricht — <b>welcher Beleg ist das</b>.
 *
 * <p><b>Der Endpunkt nimmt eine {@code MessageID} entgegen und keine Mandanten-ID</b> (Regel M1).
 * Die Ausnahmeliste in {@code docs/mandantentrennung.md} §3 bleibt damit bei zwei Eintraegen und
 * waechst hier nicht. Der Mandant kommt aus der Sitzung ueber {@code
 * MandantService.aktuellerKontext}, der den Sitzungswert gegen die zulaessige Menge prueft, statt
 * ihm zu glauben.
 *
 * <p><b>Kein Zeitfenster</b> — Regel L1 gilt fuer <i>Listen</i> ueber {@code Message}. Hier ist die
 * Menge durch einen Primaerschluessel benannt; dieselbe Begruendung wie beim Detail- und beim
 * Ketten-Endpunkt.
 *
 * <p><b>Eine fremde {@code MessageID} liefert {@code 404} — mit exakt derselben Antwort wie eine
 * erfundene.</b> Der Unterschied existiert gar nicht erst: Der Mandantenfilter steht im Statement,
 * es kommt in beiden Faellen dieselbe leere Menge zurueck (Regel M3). Kein neuer Problemtyp; {@code
 * nicht-gefunden} ist der bestehende, den auch ein unbekannter Pfad bekommt.
 *
 * <p><b>Warum ein vierter Controller unter {@code /api/nachrichten}.</b> Aus demselben Grund wie
 * beim zweiten und dritten: Liste, Detail, Kette und Belegdaten beantworten vier verschiedene
 * Fragen und haben nichts gemeinsam ausser dem Pfadpraefix. <b>Und hier kommt ein zweiter Grund
 * dazu:</b> Der Zugriff auf {@code MessageBAM} gehoert in das Fachpaket {@code bam}, nicht in
 * {@code message} — Fachpakete kennen einander nicht, und in Teil 2 steigt dasselbe Paket ueber den
 * <i>Wert</i> ein. Der Pfad folgt der fachlichen Frage und nicht der Paketstruktur (§5.1 der
 * Entwicklungsrichtlinien).
 */
@RestController
public class BamController {

  private final BamService bamService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  BamController(
      BamService bamService, MandantService mandantService, SitzungsVerwaltung sitzungsVerwaltung) {
    this.bamService = bamService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  /**
   * Die Belegnummern dieser Nachricht, nach Typ gruppiert und je Gruppe gedeckelt.
   *
   * <p><b>Die Oberflaeche ruft ihn nur, wenn {@code bamAnzahl} im Detail groesser als null ist.</b>
   * Bei 80,6 Prozent der Nachrichten und bei <i>allen</i> Merge-Eingaengen entsteht damit keine
   * Anfrage (M41). Genau dafuer traegt der Detail-Endpunkt die Zahl.
   *
   * @param messageId die {@code MessageID} aus Liste, Detail oder einem geteilten Link. Sie ist
   *     keine Berechtigung: Wer eine fremde erraet, bekommt {@code 404}
   */
  @GetMapping("/api/nachrichten/{messageId}/bam")
  public BamResponse bam(@PathVariable String messageId) {
    return bamService.werte(mandant(), messageId);
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
