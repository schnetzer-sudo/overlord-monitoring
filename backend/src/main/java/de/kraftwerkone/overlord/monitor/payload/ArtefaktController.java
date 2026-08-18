package de.kraftwerkone.overlord.monitor.payload;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rohdaten und Protokolle — drei Endpunkte unter {@code /api/nachrichten/&#123;messageId&#125;}.
 *
 * <h2>Was kein Endpunkt entgegennimmt</h2>
 *
 * <ul>
 *   <li><b>Keine Mandanten-ID</b> (Regel M1). Der Mandant kommt aus der Sitzung ueber {@code
 *       MandantService.aktuellerKontext}, der den Sitzungswert gegen die zulaessige Menge prueft,
 *       statt ihm zu glauben. Die Ausnahmeliste in {@code docs/mandantentrennung.md} §3 bleibt bei
 *       zwei Eintraegen und waechst hier nicht.
 *   <li><b>Keine Rolle.</b> Sie kommt aus dem {@code SecurityContext}. Im Altsystem entscheidet
 *       {@code request.getParameter("downloadUser")} darueber, wer das Protokoll beschnitten
 *       bekommt (Q5) — wer den Parameter aendert, aendert dort seine Rolle. Das ist der Fehler, den
 *       dieses Werkzeug ersetzt und nicht nachbaut.
 *   <li><b>Keine GUID, keine Ablagenkennung, keinen Filestore-Verweis.</b> Die {@link ArtefaktId}
 *       benennt ein Artefakt <i>innerhalb seiner Nachricht</i>; der Verweis wird serverseitig
 *       hergeleitet. Naehme ein Endpunkt ihn entgegen, waere er ein offener Proxy vor einer
 *       Produktionsablage — genau das ist im Altsystem der Fall ({@code
 *       JsonServlet.java:165}–{@code :171}).
 * </ul>
 *
 * <h2>Eine unbekannte und eine fremde Nachricht sind dieselbe Antwort</h2>
 *
 * <p>Beide {@code 404}, mit demselben Rumpf. Unterschieden sie sich, liesse sich mit einer Kennung
 * aus einer geteilten URL der fremde Bestand abfragen. Der Unterschied entsteht gar nicht erst: Der
 * Mandantenfilter steht im Statement, es kommt in beiden Faellen dieselbe leere Menge zurueck.
 *
 * <h2>Warum ein vierter Controller auf {@code /api/nachrichten}</h2>
 *
 * <p>Aus demselben Grund wie beim Nachrichtendetail und bei der Kette: Die Endpunkte teilen sich
 * den Pfadpraefix und sonst nichts. Spring loest ueber alle Controller hinweg nach dem
 * spezifischsten Muster auf; {@code /api/nachrichten}, {@code
 * /api/nachrichten/&#123;messageId&#125;} und {@code
 * /api/nachrichten/&#123;messageId&#125;/dateien} bestehen deshalb nebeneinander.
 */
@RestController
public class ArtefaktController {

  private final ArtefaktService artefaktService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;

  ArtefaktController(
      ArtefaktService artefaktService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung) {
    this.artefaktService = artefaktService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
  }

  /**
   * Die Artefakte einer Nachricht, zweigeteilt.
   *
   * <p><b>Kein Zeitfenster</b> — Regel L1 gilt fuer Listen ueber {@code Message}. Hier ist die
   * Nachricht ueber ihren Primaerschluessel benannt. Es ist auch keine Liste im Sinne von L1: Es
   * sind drei bis fuenfzehn Zeilen zu einer benannten Nachricht (M55), nicht ein Ausschnitt aus
   * einem Bestand.
   */
  @GetMapping("/api/nachrichten/{messageId}/dateien")
  public ArtefaktlisteResponse dateien(@PathVariable String messageId) {
    AngemeldeterNutzer nutzer = erforderlicherNutzer();
    return artefaktService.liste(mandant(nutzer), nutzer.rolle(), messageId);
  }

  /**
   * Ein Artefakt als Text.
   *
   * <p>Liefert <b>JSON</b> und niemals einen Bytestrom mit ratbarem Typ. Das Altsystem liefert fuer
   * Anzeige und Download denselben {@code application/octet-stream} (Q4); ein Bytestrom, dessen Typ
   * der Browser errät, ist der Weg, auf dem fremder Inhalt zu ausgefuehrtem Inhalt wird.
   */
  @GetMapping("/api/nachrichten/{messageId}/dateien/{artefaktId}/inhalt")
  public AnzeigeResponse inhalt(
      @PathVariable String messageId, @PathVariable String artefaktId, HttpServletRequest request) {
    AngemeldeterNutzer nutzer = erforderlicherNutzer();
    return artefaktService.anzeige(
        mandant(nutzer), nutzer, messageId, artefaktId, request.getRemoteAddr());
  }

  /**
   * Ein Artefakt als Datei.
   *
   * <p><b>{@code attachment}, niemals {@code inline}, und {@code application/octet-stream}, kein
   * Erraten.</b> Der Dateiname geht als {@code filename*} in {@code UTF-8} hinaus (RFC 6266) —
   * {@code ContentDisposition} baut daraus zusaetzlich eine bereinigte ASCII-Fassung fuer aeltere
   * Aufrufer.
   *
   * <p><b>Gleichlauf mit der Anzeige</b> ({@code docs/rohdaten.md} §3, Entscheidung 9): Fuer {@code
   * MANDANT} bei Protokollen ist das die beschnittene und maskierte Fassung. Es gibt keinen Pfad,
   * auf dem ein Mandantennutzer die vollstaendige Protokolldatei bekommt.
   */
  @GetMapping("/api/nachrichten/{messageId}/dateien/{artefaktId}/download")
  public ResponseEntity<Resource> download(
      @PathVariable String messageId, @PathVariable String artefaktId, HttpServletRequest request) {
    AngemeldeterNutzer nutzer = erforderlicherNutzer();
    ArtefaktService.Download datei =
        artefaktService.download(
            mandant(nutzer), nutzer, messageId, artefaktId, request.getRemoteAddr());

    ContentDisposition anhang =
        ContentDisposition.attachment().filename(datei.dateiname(), StandardCharsets.UTF_8).build();

    // X-Content-Type-Options: nosniff wird hier NICHT gesetzt: Spring Security schickt es auf
    // jeder Antwort mit, und die Kopfzeile ein zweites Mal zu setzen ergaebe sie doppelt.
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, anhang.toString())
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(datei.bytes().length)
        .body(new ByteArrayResource(datei.bytes()));
  }

  private MandantContext mandant(AngemeldeterNutzer nutzer) {
    return mandantService.aktuellerKontext(nutzer);
  }

  private AngemeldeterNutzer erforderlicherNutzer() {
    return sitzungsVerwaltung
        .aktuellerNutzer()
        .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
  }
}
