package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die Prozessansicht: <b>der Baum Partner → Richtung → Prozess</b>, mit Kennzahlen an jedem Blatt.
 *
 * <h2>Kein Mandantenparameter — in keiner Form</h2>
 *
 * <p>Regel M1. Der Mandant kommt aus der Sitzung, ueber {@code SitzungsVerwaltung} und {@code
 * MandantService}, der den Sitzungswert gegen die zulaessige Menge prueft, statt ihm zu glauben.
 * Ein {@code ?mandant=…} ist hier kein Fehler, sondern schlicht wirkungslos, und genau das prueft
 * {@code ProzessbaumIsolationDbIT}. <b>Dieser Endpunkt ist keine neue benannte Ausnahme</b> — die
 * drei, die es gibt, stehen in {@code docs/mandantentrennung.md} §3 und definieren allesamt eine
 * <i>Berechtigung</i>, statt einen Datenausschnitt abzufragen.
 *
 * <h2>Warum {@code /api/prozesse/baum} und nicht ein eigener Wurzelpfad</h2>
 *
 * <p>Die Menge ist dieselbe wie bei {@code GET /api/prozesse} — alle Prozesse des aktiven
 * Mandanten. Was sich unterscheidet, ist die <b>Form</b>: dort eine flache Auswahlliste mit drei
 * Feldern, hier ein Baum mit Kennzahlen. Ein zweiter Wurzelpfad behauptete eine zweite Ressource,
 * wo es eine zweite Sicht auf dieselbe gibt.
 *
 * <h2>Ein Parameter, und er ist freiwillig</h2>
 *
 * <p>{@code zeitraum} waehlt eines der drei Paare ({@link Rollupzeitraum}) und bestimmt damit, aus
 * welcher Rollup-Ebene die Kennzahlen kommen. Fehlt er, gilt {@code 48H} ({@code
 * ProzessbaumService.VORGABE}), und die Antwort <b>nennt das gewaehlte Paar</b> — sonst wuesste die
 * Oberflaeche nicht, was sie hervorheben und in die URL schreiben soll.
 *
 * <p><b>Er beruehrt den Umfang des Baums nicht.</b> Alle Prozesse des Mandanten stehen darin,
 * unabhaengig vom Fenster — auch die, die nie etwas getragen haben. Der Zeitraum bestimmt
 * ausschliesslich die Zahlen <i>Nachrichten</i> und <i>Fehler</i>; <i>letzte Bewegung</i> und der
 * Zustand sind fensterunabhaengig.
 *
 * <h2>Kein Eintrag in {@code SecurityConfig}, und das ist richtig</h2>
 *
 * <p>Der Pfad faellt unter {@code anyRequest().authenticated()}. Eingetragen wird dort nur, wer
 * eine <b>Rollengrenze</b> braucht — die Katalogpflege unter {@code /api/katalog/**} etwa, weil sie
 * {@code ADMIN} verlangt. Die Prozessansicht zeigt jedem angemeldeten Nutzer den Ausschnitt seines
 * aktiven Mandanten und keinen anderen.
 */
@RestController
public class ProzessbaumController {

  private final ProzessbaumService prozessbaumService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  ProzessbaumController(
      ProzessbaumService prozessbaumService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung) {
    this.prozessbaumService = prozessbaumService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  @GetMapping("/api/prozesse/baum")
  public ProzessbaumResponse baum(@RequestParam(required = false) String zeitraum) {
    MandantContext mandant = mandantService.aktuellerKontext(erforderlicherNutzer());
    return prozessbaumService.baum(mandant, Rollupzeitraum.ausCode(zeitraum));
  }

  private AngemeldeterNutzer erforderlicherNutzer() {
    return sitzungsVerwaltung
        .aktuellerNutzer()
        .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
  }
}
