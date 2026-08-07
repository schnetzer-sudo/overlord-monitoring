package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Die Fachlogik des Nachrichtendetails: Schritte benennen, den offenen Zustand bestimmen, Dauern
 * rechnen, Werte kappen.
 *
 * <p>Was hier <b>nicht</b> geschieht: klassifizieren. Die Einordnung eines {@code MessageStatus}
 * entsteht ausschliesslich im {@code MessageStatusClassifier} und wird nirgends nachgebaut — sonst
 * driftet sie ueber die Ausbaustufen auseinander.
 */
@Service
public class NachrichtendetailService {

  private final NachrichtendetailRepository detailRepository;
  private final MessageStatusClassifier statusClassifier;
  private final Clock anwendungsuhr;

  NachrichtendetailService(
      NachrichtendetailRepository detailRepository,
      MessageStatusClassifier statusClassifier,
      Clock anwendungsuhr) {
    this.detailRepository = detailRepository;
    this.statusClassifier = statusClassifier;
    this.anwendungsuhr = anwendungsuhr;
  }

  /**
   * Das Detail einer Nachricht.
   *
   * @throws RessourceNichtGefundenException {@code 404} — sowohl fuer eine erfundene {@code
   *     MessageID} als auch fuer die eines fremden Mandanten, mit <b>derselben</b> Antwort.
   *     Unterschieden sie sich, liesse sich der Bestand abfragen
   */
  public NachrichtendetailResponse detail(MandantContext mandant, String messageId) {
    NachrichtKopfZeile kopf = detailRepository.findeKopf(mandant, messageId);
    if (kopf == null) {
      throw new RessourceNichtGefundenException("Nachricht nicht sichtbar oder nicht vorhanden");
    }

    List<MessageAktion> aktionen = detailRepository.findeAktionen(mandant, messageId);
    Schrittnamen namen = Schrittnamen.aus(detailRepository.findeAblaufschritte(mandant, messageId));

    MessageStatusKind einordnung = statusClassifier.einordnung(kopf.status());
    OffenerZustand zustand = zustand(einordnung, aktionen);
    ZoneId zone = anwendungsuhr.getZone();

    return new NachrichtendetailResponse(
        kopf.messageId(),
        kopf.status(),
        einordnung.name(),
        kopf.processId(),
        kopf.processName(),
        kopf.projectName(),
        kopf.sosName(),
        Zeitpunkte.nachUtc(kopf.zeitpunkt(), zone),
        Zeitpunkte.nachUtc(fachlicherStart(aktionen), zone),
        kopf.timeoutSekunden() == null ? null : (int) kopf.timeoutSekunden(),
        kopf.eigenschaftenAnzahl(),
        zustand,
        zustand == OffenerZustand.WARTET_VOR ? kopf.naechsterSchrittName() : null,
        schrittfolge(aktionen, namen, zustand, zone),
        kuratierte(detailRepository.findeKuratierteEigenschaften(mandant, messageId)));
  }

  /**
   * Alle technischen Eigenschaften einer Nachricht.
   *
   * <p>Die Existenz wird <b>zuerst</b> geprueft und nicht aus der Zeilenzahl geschlossen: Eine
   * leere Liste waere eine andere Auskunft als {@code 404} und damit eine Auskunft ueber den
   * Bestand.
   */
  public List<EigenschaftResponse> eigenschaften(MandantContext mandant, String messageId) {
    if (!detailRepository.existiert(mandant, messageId)) {
      throw new RessourceNichtGefundenException("Nachricht nicht sichtbar oder nicht vorhanden");
    }
    return detailRepository.findeEigenschaften(mandant, messageId).stream()
        .map(NachrichtendetailService::gekappt)
        .toList();
  }

  /**
   * Der offene Zustand — die vier Faelle in der Reihenfolge, in der sie sich ausschliessen.
   *
   * <p>„Ohne Aktion" muss <b>vor</b> „jede Aktion beendet" stehen: Ueber einer leeren Menge ist
   * „jede ist beendet" wahr, und die Nachricht bekaeme {@link OffenerZustand#WARTET_VOR} samt einem
   * naechsten Schritt, vor dem sie gar nicht steht.
   *
   * <p>Geprueft wird ueber <b>alle</b> Aktionen einschliesslich des Metadaten-Schritts, denn M16
   * (3) hat genau so gemessen — und eine Nachricht, die nur ihren Metadaten-Schritt hat, hat sehr
   * wohl eine Aktion.
   */
  private OffenerZustand zustand(MessageStatusKind einordnung, List<MessageAktion> aktionen) {
    boolean offen =
        einordnung == MessageStatusKind.WARTEND || einordnung == MessageStatusKind.LAEUFT;
    if (!offen) {
      return OffenerZustand.KEINER;
    }
    if (aktionen.isEmpty()) {
      return OffenerZustand.OHNE_SCHRITT;
    }
    return aktionen.stream().anyMatch(MessageAktion::ohneEnde)
        ? OffenerZustand.LAEUFT_AUF
        : OffenerZustand.WARTET_VOR;
  }

  /**
   * Der fachliche Start: {@code MIN(MessageAction.MessageActionStart)} (Regel Q2), gerechnet aus
   * den ohnehin geladenen Aktionen statt in einer zweiten Abfrage.
   *
   * <p><b>Ueber alle Aktionen, auch den Metadaten-Schritt.</b> An ihm kommt die Nachricht ins
   * System (M17 3: dort haengen die {@code Message.*}-Eigenschaften, dort stehen die {@code
   * *Reader}-Dienste); ihn auszunehmen ergaebe einen zu spaeten Start.
   */
  private static LocalDateTime fachlicherStart(List<MessageAktion> aktionen) {
    return aktionen.stream()
        .map(MessageAktion::start)
        .filter(Objects::nonNull)
        .min(Comparator.naturalOrder())
        .orElse(null);
  }

  /**
   * Die Schrittfolge — <b>ohne</b> den Metadaten-Schritt und ohne Obergrenze.
   *
   * <p>Die Sortierung kommt aus der Abfrage und wird hier nicht wiederholt.
   */
  private static List<SchrittResponse> schrittfolge(
      List<MessageAktion> aktionen, Schrittnamen namen, OffenerZustand zustand, ZoneId zone) {
    List<SchrittResponse> schritte = new ArrayList<>(aktionen.size());
    for (MessageAktion aktion : aktionen) {
      if (aktion.istMetadatenSchritt()) {
        continue;
      }
      Schrittname name = namen.loese(aktion.sosId(), aktion.sosActionId(), aktion.bausteine());
      schritte.add(
          new SchrittResponse(
              aktion.messageActionId(),
              name.name(),
              name.herkunft(),
              name.rohwert(),
              Zeitpunkte.nachUtc(aktion.start(), zone),
              Zeitpunkte.nachUtc(aktion.ende(), zone),
              dauerSekunden(aktion),
              aktion.timeoutSekunden() == null ? null : (int) aktion.timeoutSekunden(),
              zustand == OffenerZustand.LAEUFT_AUF && aktion.ohneEnde()));
    }
    return List.copyOf(schritte);
  }

  /**
   * Die Dauer eines Schritts in ganzen Sekunden, im Backend gerechnet (Richtlinie §5.3).
   *
   * <p><b>Eine negative Dauer wird zu {@code null} und nicht zu einer negativen Zahl.</b> Im
   * dichten Tag kommt sie auf keiner der 20.352 Aktionen vor — M16 (1) hat ausdruecklich darauf
   * geprueft und {@code 0} gemessen, Start und Ende stehen durchgaengig in der richtigen
   * Reihenfolge. Die Regel bleibt trotzdem: Sie kostet eine Zeile, und eine Zeitleiste, die „minus
   * drei Sekunden" anzeigt, ist schlechter als eine, die an dieser Stelle nichts sagt.
   */
  private static Long dauerSekunden(MessageAktion aktion) {
    if (aktion.start() == null || aktion.ende() == null) {
      return null;
    }
    long sekunden = Duration.between(aktion.start(), aktion.ende()).toSeconds();
    return sekunden < 0 ? null : sekunden;
  }

  /**
   * Die kuratierten Eigenschaften — <b>leere Werte fallen heraus</b>.
   *
   * <p>Kein Feld mit leerer Zeichenkette, kein {@code null} als Platzhalter: Die Oberflaeche soll
   * gar nicht erst in die Lage kommen, eine leere Zeile zu zeichnen. Genau daran ist die BAM-Spalte
   * in Schritt 4 gescheitert.
   *
   * <p>Sortiert nach dem Rang aus {@link KuratierteEigenschaften} — die Anzeigereihenfolge steht
   * damit an einer Stelle und nicht zusaetzlich in der Oberflaeche.
   */
  private static List<KuratierteEigenschaftResponse> kuratierte(List<MessageEigenschaft> gelesen) {
    return gelesen.stream()
        .filter(eigenschaft -> eigenschaft.wert() != null && !eigenschaft.wert().isBlank())
        .map(
            eigenschaft ->
                new KuratierteEigenschaftResponse(
                    eigenschaft.name(),
                    eigenschaft.wert(),
                    KuratierteEigenschaften.rang(eigenschaft.name())))
        .sorted(Comparator.comparingInt(KuratierteEigenschaftResponse::rang))
        .toList();
  }

  /**
   * Die harte Kappung eines Eigenschaftswerts auf {@link
   * NachrichtendetailRepository#WERT_GRENZE_BYTES} Bytes.
   *
   * <p>Gekappt wird <b>auf einer Zeichengrenze</b>: Ein einfacher Byte-Schnitt koennte mitten in
   * eine UTF-8-Folge fallen und ein Ersatzzeichen erzeugen, das im Wert nie stand. Deshalb wird von
   * der Grenze so weit zurueckgegangen, bis kein Folgebyte mehr dasteht.
   *
   * <p>Die <b>ungekappte</b> Laenge kommt aus der Datenbank und nicht aus dem gelesenen Wert — der
   * ist bereits in der Abfrage begrenzt und wuesste seine eigene urspruengliche Groesse nicht.
   */
  static EigenschaftResponse gekappt(MessageEigenschaft eigenschaft) {
    String wert = eigenschaft.wert();
    if (wert == null) {
      return new EigenschaftResponse(
          eigenschaft.name(), null, eigenschaft.messageActionId(), false, null);
    }
    byte[] roh = wert.getBytes(StandardCharsets.UTF_8);
    int grenze = NachrichtendetailRepository.WERT_GRENZE_BYTES;
    if (roh.length <= grenze && eigenschaft.laengeBytes() <= grenze) {
      return new EigenschaftResponse(
          eigenschaft.name(), wert, eigenschaft.messageActionId(), false, null);
    }

    // Schnittstelle suchen: roh[ende] ist das erste Byte, das NICHT mehr mitkommt. Ist es ein
    // Folgebyte, steht der Schnitt mitten in einem Zeichen — dann so weit zurueck, bis er auf
    // einer Zeichengrenze liegt.
    int ende = Math.min(roh.length, grenze);
    while (ende > 0 && ende < roh.length && istFolgebyte(roh[ende])) {
      ende--;
    }
    return new EigenschaftResponse(
        eigenschaft.name(),
        new String(roh, 0, ende, StandardCharsets.UTF_8),
        eigenschaft.messageActionId(),
        true,
        eigenschaft.laengeBytes());
  }

  /** Ein UTF-8-Folgebyte traegt das Bitmuster {@code 10xxxxxx}. */
  private static boolean istFolgebyte(byte wert) {
    return (wert & 0xC0) == 0x80;
  }
}
