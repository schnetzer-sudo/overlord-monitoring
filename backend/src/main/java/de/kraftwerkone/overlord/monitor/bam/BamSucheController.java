package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import java.time.Clock;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die BAM-Suche — <b>wer eine Belegnummer hat, findet die Nachrichten, auf denen sie steht</b>.
 * Auch ohne die führende Null, und auch mit mehreren Nummern gleichzeitig.
 *
 * <p><b>Er nimmt keine Mandanten-ID entgegen</b> (Regel M1). Der Mandant kommt aus der Sitzung, und
 * zwar über {@code MandantService}: Der prüft den Sitzungswert gegen die zulässige Menge, statt ihm
 * zu glauben. Die Ausnahmeliste in {@code docs/mandantentrennung.md} §3 bleibt bei zwei Einträgen
 * und wächst hier nicht.
 *
 * <p><b>Die Parameter sind roh und werden hier nicht ausgewertet.</b> Geprüft wird an einer Stelle,
 * in {@link BamSuchfilter#aus}; alles Unbrauchbare ist dort {@code 400} mit eigenem Problemtyp. Der
 * Controller übersetzt nur HTTP.
 *
 * <p><b>Warum {@code /api/bam/suche} und nicht {@code /api/nachrichten/suche}.</b> Der Endpunkt
 * beantwortet eine eigene fachliche Frage — <i>wo steht diese Nummer</i> — und nicht eine Spielart
 * der Liste. Er hat kein Zeitraum-Kürzel, keinen Status- und keinen Prozessfilter, keinen Cursor
 * und ein anderes Standardfenster; unter {@code /api/nachrichten} sähe er aus wie derselbe Endpunkt
 * mit anderen Parametern. <b>Der Belegdaten-Block aus Teil 1 liegt aus dem umgekehrten Grund unter
 * {@code /api/nachrichten}</b>: Er beantwortet eine Frage <i>zu einer benannten Nachricht</i>.
 */
@RestController
public class BamSucheController {

  private final BamSucheService bamSucheService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;
  private final Clock anwendungsuhr;

  BamSucheController(
      BamSucheService bamSucheService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung,
      Clock anwendungsuhr) {
    this.bamSucheService = bamSucheService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
    this.anwendungsuhr = anwendungsuhr;
  }

  /**
   * @param begriff <b>wiederholt</b>, je in der Form {@code <typ>:<wert>}. Der Doppelpunkt ist
   *     Pflicht; ohne Typ lautet der Parameter {@code :4711815}. Mindestens einer, höchstens {@link
   *     BamSuchfilter#HOECHSTENS_BEGRIFFE}
   * @param von Beginn des Zeitfensters, ISO 8601 in UTC — nur zusammen mit {@code bis}
   * @param bis Ende des Zeitfensters. Fehlen beide, gilt {@link BamSuchfilter#FENSTER_VORGABE}; das
   *     tatsächlich verwendete Fenster steht in der Antwort
   */
  @GetMapping("/api/bam/suche")
  public BamSucheResponse suche(
      @RequestParam(required = false) List<String> begriff,
      @RequestParam(required = false) String von,
      @RequestParam(required = false) String bis) {

    MandantContext mandant = mandantService.aktuellerKontext(erforderlicherNutzer());
    return bamSucheService.suche(mandant, BamSuchfilter.aus(begriff, von, bis, anwendungsuhr));
  }

  private AngemeldeterNutzer erforderlicherNutzer() {
    return sitzungsVerwaltung
        .aktuellerNutzer()
        .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
  }
}
