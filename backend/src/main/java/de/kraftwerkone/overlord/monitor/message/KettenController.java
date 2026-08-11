package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.Seite;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die Verkettung — <b>was an dieser Nachricht haengt</b>.
 *
 * <p><b>Beide Endpunkte nehmen eine {@code MessageID} entgegen und keine Mandanten-ID</b> (Regel
 * M1). Die Ausnahmeliste in {@code docs/mandantentrennung.md} §3 bleibt damit bei zwei Eintraegen
 * und waechst hier nicht. Der Mandant kommt aus der Sitzung ueber {@code
 * MandantService.aktuellerKontext}, der den Sitzungswert gegen die zulaessige Menge prueft, statt
 * ihm zu glauben.
 *
 * <p><b>Kein Zeitfenster</b> — Regel L1 gilt fuer <i>Listen</i> ueber {@code Message}. Hier ist die
 * Menge durch einen Primaerschluessel benannt; dieselbe Begruendung wie beim Detail-Endpunkt. Beim
 * Blaetter-Endpunkt kaeme ein Fenster sogar einer falschen Antwort gleich: Es schnitte gerade die
 * Kinder ab, die ausserhalb liegen.
 *
 * <p><b>Die zweite Route heisst {@code …/kette/abwaerts} — umbenannt am 11.08.2026.</b> Ihr
 * frueherer Name behauptete eine Bedeutung, die nur bei der Aufteilung stimmt: Beim Merge-Ergebnis
 * stehen dort seine <i>Eingaenge</i>, also das, woher es kommt. Der alte Pfad ist <b>entfernt</b>
 * und nicht als Weiche behalten — zwei Pfade auf dasselbe waeren genau die Drift, gegen die die
 * Umbenennung antritt. Woher sie kommt, steht datiert in {@code docs/verkettung.md} §2.
 *
 * <p><b>Eine fremde {@code MessageID} liefert {@code 404} — mit exakt derselben Antwort wie eine
 * erfundene.</b> Der Unterschied existiert gar nicht erst: Der Mandantenfilter steht im Statement,
 * es kommt in beiden Faellen dieselbe leere Menge zurueck (Regel M3). Kein neuer Problemtyp; {@code
 * nicht-gefunden} ist der bestehende, den auch ein unbekannter Pfad bekommt.
 *
 * <p><b>Warum ein dritter Controller unter {@code /api/nachrichten}.</b> Aus demselben Grund, aus
 * dem es einen zweiten gibt: Liste, Detail und Kette beantworten drei verschiedene Fragen — „wo
 * steht mein Beleg", „was ist im Einzelnen passiert", „was haengt daran" — und haben nichts
 * gemeinsam ausser dem Pfadpraefix. Spring loest ueber alle Controller hinweg auf und bevorzugt das
 * woertliche Segment vor der Pfadvariablen; {@code KettenDbIT} haelt das fest, damit es bei einer
 * Umstellung nicht still kippt.
 */
@RestController
public class KettenController {

  private final KettenService kettenService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  KettenController(
      KettenService kettenService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung) {
    this.kettenService = kettenService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  /**
   * Die Kette einer Nachricht: <b>der Aufstieg vollstaendig, der Abstieg eine Ebene</b>.
   *
   * @param messageId die {@code MessageID} aus Liste oder Detail. Sie ist keine Berechtigung: Wer
   *     eine fremde erraet, bekommt {@code 404}
   */
  @GetMapping("/api/nachrichten/{messageId}/kette")
  public KetteResponse kette(@PathVariable String messageId) {
    return kettenService.kette(mandant(), messageId);
  }

  /**
   * Eine weitere Seite der Abwaertsglieder — <b>fuer die Glieder, die selbst breit sind</b>.
   *
   * <p>Bis zu 3.350 Kinder an einer Wurzel und 897 Eingaenge an einem Merge-Ergebnis (M30‑2); die
   * Kette liefert davon 50. Wer mehr sehen will, blaettert hier weiter — cursor-basiert ueber
   * {@code (MessageLastUpdate, MessageID)} und niemals ueber {@code OFFSET} (Regel L3).
   *
   * @param cursor undurchsichtige Seitenposition der vorigen Antwort. Fuer die zweite Seite ist das
   *     {@code abwaertsCursor} aus der Ketten-Antwort — deshalb kostet der erste Klick auf „Mehr
   *     laden" eine Anfrage und nicht zwei. Unlesbar ist {@code 400} und nicht stillschweigend „von
   *     vorne"
   * @param limit Seitengroesse; Vorgabe 50, Maximum wie in der Liste
   */
  @GetMapping("/api/nachrichten/{messageId}/kette/abwaerts")
  public Seite<KettengliedResponse> abwaerts(
      @PathVariable String messageId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer limit) {
    return kettenService.abwaerts(mandant(), messageId, cursor, limit);
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
