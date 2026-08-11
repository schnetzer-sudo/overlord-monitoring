package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Das Nachrichtendetail — was mit <b>einer</b> Nachricht im Einzelnen passiert ist.
 *
 * <p><b>Beide Endpunkte nehmen eine {@code MessageID} entgegen und keine Mandanten-ID</b> (Regel
 * M1). Die Ausnahmeliste in {@code docs/mandantentrennung.md} §3 bleibt damit bei zwei Eintraegen
 * und waechst hier nicht. Der Mandant kommt aus der Sitzung ueber {@code
 * MandantService.aktuellerKontext}, der den Sitzungswert gegen die zulaessige Menge prueft, statt
 * ihm zu glauben.
 *
 * <p><b>Eine fremde {@code MessageID} liefert {@code 404} — mit exakt derselben Antwort wie eine
 * erfundene.</b> Unterschieden sie sich, liesse sich der Bestand abfragen. Der Unterschied
 * existiert gar nicht erst: Der Mandantenfilter steht im Statement, es kommt in beiden Faellen
 * dieselbe leere Menge zurueck.
 *
 * <p><b>Warum ein eigener Controller neben {@code NachrichtenController}.</b> Beide bedienen {@code
 * /api/nachrichten}, aber sie beantworten verschiedene Fragen — „wo steht mein Beleg" gegen „was
 * ist im Einzelnen passiert" — und haben nichts gemeinsam ausser dem Pfadpraefix. Dass {@code
 * /api/nachrichten} aus dem Nachbarcontroller, {@code /api/nachrichten/&#123;messageId&#125;} von
 * hier und {@code /api/nachrichten/&#123;messageId&#125;/kette} aus dem dritten nebeneinander
 * bestehen koennen, ist kein Zufall, auf den man hofft: Spring loest ueber alle Controller hinweg
 * auf. Ein Test haelt das fest, damit es nicht bei einer Umstellung still kippt.
 *
 * <p><b>Bis zum 11.08.2026 stand hier ein schaerferer Fall:</b> {@code /api/nachrichten/merkmale}
 * war ein woertliches Segment an derselben Stelle wie die Pfadvariable, und der Vorrang des
 * woertlichen Segments entschied. Der Endpunkt ist mit dem Ausblende-Schalter entfallen
 * (docs/nachrichtenliste.md §5); die Regel selbst gilt unveraendert weiter und traegt jetzt {@code
 * …/kette}.
 */
@RestController
public class NachrichtendetailController {

  private final NachrichtendetailService detailService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  NachrichtendetailController(
      NachrichtendetailService detailService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung) {
    this.detailService = detailService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  /**
   * Kopf, Schrittfolge und die kuratierten Eigenschaften einer Nachricht.
   *
   * <p><b>Kein Zeitfenster</b> — Regel L1 gilt fuer Listen ueber {@code Message}. Hier ist die
   * Nachricht ueber ihren Primaerschluessel benannt.
   *
   * @param messageId die {@code MessageID} aus der Liste. Sie ist keine Berechtigung: Wer eine
   *     fremde erraet, bekommt {@code 404}
   */
  @GetMapping("/api/nachrichten/{messageId}")
  public NachrichtendetailResponse detail(@PathVariable String messageId) {
    return detailService.detail(mandant(), messageId);
  }

  /**
   * Alle technischen Eigenschaften einer Nachricht — <b>auf Abruf</b>, nicht im Detail.
   *
   * <p><b>Warum getrennt.</b> Gemessen sind rund 22,6 Eigenschaften und 595 Byte je Nachricht (M17)
   * — es geht also nicht um Megabyte. Getrennt sind sie trotzdem, weil sie im Detail nicht
   * gebraucht werden: Der Kopf traegt die <i>Anzahl</i>, die Oberflaeche beschriftet damit einen
   * eingeklappten Block, und geladen wird er erst, wenn ihn jemand aufklappt. Waere die Anzahl
   * nicht im Kopf, muesste die Oberflaeche zum Beschriften laden — und der zweite Endpunkt haette
   * keinen Zweck.
   */
  @GetMapping("/api/nachrichten/{messageId}/eigenschaften")
  public List<EigenschaftResponse> eigenschaften(@PathVariable String messageId) {
    return detailService.eigenschaften(mandant(), messageId);
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
