package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.Seite;
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
 * Die Nachrichtenliste — der erste fachliche Endpunkt des Werkzeugs.
 *
 * <p><b>Er nimmt keine Mandanten-ID entgegen</b> (Regel M1). Der Mandant kommt aus der Sitzung, und
 * zwar ueber {@code MandantService}: Der prueft den Sitzungswert gegen die zulaessige Menge, statt
 * ihm zu glauben. Verliert ein Nutzer waehrend einer laufenden Sitzung seine Zuordnung, faellt der
 * aktive Mandant damit von selbst weg und der Endpunkt antwortet {@code kein-mandant-gewaehlt}.
 *
 * <p><b>Die Parameter sind roh und werden hier nicht ausgewertet.</b> Geprueft wird an einer
 * Stelle, in {@link NachrichtenFilter#aus}; alles Unbrauchbare ist dort {@code 400} mit eigenem
 * Problemtyp. Der Controller uebersetzt nur HTTP.
 *
 * <p><b>Der Cursor gehoert nicht in eine geteilte URL.</b> Er wird als Parameter angenommen, weil
 * das Blaettern ihn braucht — aber Filter und Zeitfenster sind das, was ein Kollege im Link sehen
 * soll. Ein Link auf Seite sieben eines relativen Fensters zeigt beim Empfaenger auf andere Zeilen.
 */
@RestController
public class NachrichtenController {

  private final NachrichtenService nachrichtenService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;
  private final Clock anwendungsuhr;

  NachrichtenController(
      NachrichtenService nachrichtenService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung,
      Clock anwendungsuhr) {
    this.nachrichtenService = nachrichtenService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
    this.anwendungsuhr = anwendungsuhr;
  }

  /**
   * @param zeitraum {@code 24h}, {@code 7d} oder {@code 30d}; Vorgabe {@code 24h}. Schliesst {@code
   *     von}/{@code bis} aus.
   * @param von absoluter Beginn, ISO 8601 in UTC — nur zusammen mit {@code bis}
   * @param status mehrfach; Werte aus {@code MessageStatusKind}, nicht Rohwerte
   * @param prozess mehrfach; {@code ProcessID}
   * @param suche Freitext auf Prozess-, Projekt- und Ablaufnamen, mindestens drei Zeichen
   * @param langeSuche hebt die Fenstergrenze der Suche auf; Vorgabe {@code false}. Wirkt nur
   *     zusammen mit {@code suche} und nur bis {@link NachrichtenFilter#SUCHE_FENSTER_LANG}.
   * @param zwischenschritte ob {@code SPLITTED}/{@code MERGED} mitkommen; Vorgabe {@code false}
   * @param cursor undurchsichtige Seitenposition der vorigen Antwort
   */
  @GetMapping("/api/nachrichten")
  public Seite<NachrichtResponse> liste(
      @RequestParam(required = false) String zeitraum,
      @RequestParam(required = false) String von,
      @RequestParam(required = false) String bis,
      @RequestParam(required = false) List<String> status,
      @RequestParam(required = false) List<String> prozess,
      @RequestParam(required = false) String suche,
      @RequestParam(required = false) Boolean langeSuche,
      @RequestParam(required = false) Boolean zwischenschritte,
      @RequestParam(required = false) String sortierung,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer limit) {

    MandantContext mandant = mandantService.aktuellerKontext(erforderlicherNutzer());
    NachrichtenFilter filter =
        NachrichtenFilter.aus(
            zeitraum,
            von,
            bis,
            status,
            prozess,
            suche,
            langeSuche,
            zwischenschritte,
            sortierung,
            cursor,
            limit,
            anwendungsuhr);
    return nachrichtenService.liste(mandant, filter);
  }

  private AngemeldeterNutzer erforderlicherNutzer() {
    return sitzungsVerwaltung
        .aktuellerNutzer()
        .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
  }
}
