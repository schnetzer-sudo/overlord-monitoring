package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.Kettenrollen;
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
 * <p>Was hier <b>nicht</b> geschieht: klassifizieren und Rollen bestimmen. Die Einordnung eines
 * {@code MessageStatus} entsteht ausschliesslich im {@code MessageStatusClassifier}, die
 * Kettenrollen ausschliesslich in {@code common/Kettenrollen}. Beides wird hier benutzt und
 * nirgends nachgebaut — sonst driftet es ueber die Ausbaustufen auseinander.
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
    List<MessageAktion> schrittfolge = schrittfolge(aktionen);
    OffenerZustand zustand = zustand(einordnung, kopf, aktionen, schrittfolge);
    ZoneId zone = anwendungsuhr.getZone();
    LocalDateTime jetzt = LocalDateTime.now(anwendungsuhr);
    Integer frist = frist(kopf, einordnung);
    LocalDateTime start = fachlicherStart(aktionen);

    return new NachrichtendetailResponse(
        kopf.messageId(),
        kopf.status(),
        einordnung.name(),
        kopf.processId(),
        kopf.processName(),
        kopf.projectName(),
        kopf.sosName(),
        // Die Rollen entstehen in `common/Kettenrollen` und werden hier nicht nachgebaut —
        // dieselbe Bauform wie beim MessageStatusClassifier. `List.copyOf` eines EnumSet behaelt
        // die Deklarationsreihenfolge von Kettenrolle; die Reihenfolge in der Antwort haengt damit
        // nicht daran, in welcher Reihenfolge die Spalten gelesen wurden.
        List.copyOf(
            Kettenrollen.aus(
                kopf.source(), kopf.sourceMessageId(), kopf.targetMessageId(), kopf.target())),
        Zeitpunkte.nachUtc(kopf.zeitpunkt(), zone),
        Zeitpunkte.nachUtc(start, zone),
        abstand(start, kopf.zeitpunkt()),
        frist,
        kopf.eigenschaftenAnzahl(),
        // Die Zahl der BAM-Werte, damit die Oberflaeche den Block gar nicht erst zeichnet, wo es
        // nichts zu zeigen gibt — bei 80,6 Prozent der Nachrichten ist das der Fall (M41).
        kopf.bamAnzahl(),
        zustand,
        zustand.istWartend() ? kopf.naechsterSchrittName() : null,
        wartetSeitSekunden(zustand, aktionen, schrittfolge, jetzt),
        schrittResponses(schrittfolge, namen, zustand, zone),
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
   * Der offene Zustand — die sechs Faelle in der Reihenfolge, in der sie sich ausschliessen.
   *
   * <p>„Keine Schrittfolge" muss <b>vor</b> „jede Aktion beendet" stehen: Ueber einer leeren Menge
   * ist „jede ist beendet" wahr, und die Nachricht bekaeme einen Wartezustand samt einem Schritt,
   * an dem sie gar nicht steht.
   *
   * <p><b>Geprueft wird ueber die Schrittfolge, also ohne den Metadaten-Schritt</b> (geaendert am
   * 10.08.2026 mit M29). Bis dahin lief die Pruefung ueber <b>alle</b> Aktionen, weil M16 (3) so
   * gemessen hat. Der Wechsel ist kein Widerspruch zu jener Messung, sondern die Folge derselben
   * Trennung, die diese Runde ueberall zieht: Der Metadaten-Schritt erscheint nicht in der
   * Zeitleiste (S1). Eine Nachricht, die nur ihn hat, hat fuer den Nutzer keinen Schritt — sie als
   * {@link OffenerZustand#LAEUFT_AUF} zu fuehren hiesse, eine Zeile zu markieren, die niemand
   * sieht. <b>An den Daten der Testkopie aendert das nichts:</b> Keine der 538 wartenden
   * Nachrichten hat ausschliesslich ihren Metadaten-Schritt (M29 1, {@code 0} von 538).
   *
   * <p><b>Die leere Schrittfolge traegt zwei Faelle, und sie werden getrennt</b> (aufgeteilt am
   * 10.08.2026, vorher {@code OHNE_SCHRITT}). Gibt es den Metadaten-Schritt, ist die Nachricht
   * angekommen und seitdem nicht weitergelaufen ({@link OffenerZustand#EMPFANGEN}); gibt es gar
   * keine Aktion, ist zu ihr kein Ablauf protokolliert ({@link OffenerZustand#OHNE_AKTION}). Das
   * erste ist eine Auskunft ueber die Plattform, das zweite ueber die Datenlage.
   *
   * <p><b>Die Aufteilung {@code WARTET_IN} gegen {@code WARTET_VOR} faellt hier und nicht in der
   * Oberflaeche.</b> Verglichen werden {@code Message.SOSID}/{@code SOSActionID} mit dem zuletzt
   * ausgefuehrten Schritt — ueber die <b>Kennungen</b>, nicht ueber den Namen: Zwei Schritte
   * desselben Ablaufs koennen gleich heissen, und dann waere ein Namensvergleich eine Verwechslung.
   */
  private OffenerZustand zustand(
      MessageStatusKind einordnung,
      NachrichtKopfZeile kopf,
      List<MessageAktion> aktionen,
      List<MessageAktion> schrittfolge) {
    boolean offen =
        einordnung == MessageStatusKind.WARTEND || einordnung == MessageStatusKind.LAEUFT;
    if (!offen) {
      return OffenerZustand.KEINER;
    }
    if (schrittfolge.isEmpty()) {
      return aktionen.isEmpty() ? OffenerZustand.OHNE_AKTION : OffenerZustand.EMPFANGEN;
    }
    if (schrittfolge.stream().anyMatch(MessageAktion::ohneEnde)) {
      return OffenerZustand.LAEUFT_AUF;
    }
    return zeigtAuf(kopf, letzterSchritt(schrittfolge))
        ? OffenerZustand.WARTET_IN
        : OffenerZustand.WARTET_VOR;
  }

  /**
   * Zeigt der Verweis der Nachricht auf genau diesen Schritt?
   *
   * <p>Beide Haelften des zusammengesetzten Schluessels muessen stimmen: 2,51 Prozent der
   * Nachrichten haben Schritte aus mehr als einem Ablauf (M20), und eine {@code SOSActionID} allein
   * ist dann nicht eindeutig.
   *
   * <p>Ist {@code Message.SOSID} leer, zeigt der Verweis auf nichts — das ergibt {@link
   * OffenerZustand#WARTET_VOR} mit einem {@code naechsterSchritt} von {@code null}, und die
   * Oberflaeche benennt genau das. Ueber den Gesamtbestand laeuft dieser Verweis zu 43,9 Prozent
   * ins Leere (M13).
   */
  private static boolean zeigtAuf(NachrichtKopfZeile kopf, MessageAktion schritt) {
    return kopf.sosActionId() != null
        && Objects.equals(kopf.sosId(), schritt.sosId())
        && kopf.sosActionId() == schritt.sosActionId();
  }

  /**
   * Der <b>zuletzt ausgefuehrte</b> Schritt der Schrittfolge.
   *
   * <p>Die Ordnung ist die der Zeitleiste — {@code MessageActionStart}, bei Gleichstand {@code
   * MessageActionID} — und sie steht an <b>genau einer</b> Stelle: in der Abfrage ({@code
   * NachrichtendetailRepository.findeAktionen}). Hier wird nur das letzte Element genommen, nicht
   * ein zweites Mal sortiert. Ein zweites Sortierkriterium an einer zweiten Stelle waere genau die
   * Drift, gegen die diese Regel gerichtet ist.
   *
   * <p>M29 (0) hat nachgemessen, dass diese Ordnung und {@code MAX(MessageActionID)} auf allen 538
   * wartenden Nachrichten dieselbe Zeile treffen. Das ist ein Befund ueber die Daten und keine
   * Freigabe, hier die andere Ordnung zu nehmen.
   */
  private static MessageAktion letzterSchritt(List<MessageAktion> schrittfolge) {
    return schrittfolge.get(schrittfolge.size() - 1);
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
   * <p><b>Die eine Stelle, an der der Metadaten-Schritt ausgenommen wird.</b> Zustand, Wartedauer
   * und Anzeige lesen alle dieselbe Liste; wuerde jede fuer sich filtern, gaebe es drei Stellen, an
   * denen dieselbe Regel driften kann. Kriterium ist {@code SOSActionID = 0} (S1).
   *
   * <p>Die Sortierung kommt aus der Abfrage und wird hier nicht wiederholt.
   */
  private static List<MessageAktion> schrittfolge(List<MessageAktion> aktionen) {
    return aktionen.stream().filter(aktion -> !aktion.istMetadatenSchritt()).toList();
  }

  /** Die Schrittfolge als Antwort — Namen, Zeiten, Dauern. */
  private static List<SchrittResponse> schrittResponses(
      List<MessageAktion> schrittfolge, Schrittnamen namen, OffenerZustand zustand, ZoneId zone) {
    List<SchrittResponse> schritte = new ArrayList<>(schrittfolge.size());
    for (MessageAktion aktion : schrittfolge) {
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
   * Wie lange die Nachricht schon steht — <b>gegen die Anwendungsuhr</b>, niemals im Browser
   * gerechnet.
   *
   * <p>Die Uhr ist die Anwendungsuhr und nicht die Systemuhr: Die Ausnahme in Regel A5 gilt fuer
   * <i>sicherheitsrelevante</i> Zeit, also Sperrfristen und Sitzungsablauf. Im Profil {@code dev}
   * steht sie um Monate zurueck, und genau deshalb darf diese Zahl nicht im Browser entstehen —
   * dort ergaebe {@code Date.now()} gegen einen gelieferten Zeitstempel Monate statt Stunden.
   *
   * <p>Bewusst ein vollstaendiges {@code switch} ohne {@code default}: Ein siebter Zustand soll
   * hier einen Compilerfehler ausloesen und keine stille {@code null} erben. Genau das hat die
   * Aufteilung von {@code OHNE_SCHRITT} am 10.08.2026 erzwungen — die beiden neuen Faelle konnten
   * keine Voreinstellung erben, sie mussten hier bewusst entschieden werden.
   */
  private static Long wartetSeitSekunden(
      OffenerZustand zustand,
      List<MessageAktion> aktionen,
      List<MessageAktion> schrittfolge,
      LocalDateTime jetzt) {
    return switch (zustand) {
      // Ohne jede Aktion gibt es keinen Anker: Der fachliche Start ist dort ebenfalls null, und
      // eine Dauer aus MessageLastUpdate waere eine erfundene Zahl — der Zeitpunkt der letzten
      // Aenderung ist nicht der Zeitpunkt des Eingangs.
      case KEINER, OHNE_AKTION -> null;
      // Angekommen und seitdem nicht weitergelaufen: gerechnet ab dem Metadaten-Schritt. Er ist
      // kein Verarbeitungsschritt, aber er IST das Ereignis mit einem Zeitpunkt (M17 3) — aus
      // demselben Grund rechnet der fachliche Start ueber ihn. Mit null sagte das Feld an der
      // einzigen Stelle nichts, an der es etwas zu sagen haette.
      case EMPFANGEN ->
          aktionen.stream()
              .filter(MessageAktion::istMetadatenSchritt)
              .findFirst()
              // Ende, und ist es nicht gesetzt, der Beginn — dieselbe Zeile also, nicht ein
              // anderer Anker.
              .map(schrittNull -> abstand(bezugspunkt(schrittNull), jetzt))
              .orElse(null);
      case LAEUFT_AUF ->
          schrittfolge.stream()
              .filter(MessageAktion::ohneEnde)
              .findFirst()
              .map(offene -> abstand(offene.start(), jetzt))
              .orElse(null);
      case WARTET_IN, WARTET_VOR -> abstand(letzterSchritt(schrittfolge).ende(), jetzt);
    };
  }

  /** Ende einer Aktion, ersatzweise ihr Beginn. Beide duerfen {@code NULL} sein. */
  private static LocalDateTime bezugspunkt(MessageAktion aktion) {
    return aktion.ende() != null ? aktion.ende() : aktion.start();
  }

  /**
   * Die Frist der Nachricht: {@code Message.MessageTimeout} in <b>Sekunden</b> (Regel Z2, M8).
   *
   * <h2>Bei {@link MessageStatusKind#WARTEND} ist sie {@code null} — seit dem 03.09.2026 (E‑76)
   * </h2>
   *
   * <p><b>Bis dahin stand dort {@code 1800}</b>, und das war eine falsche Auskunft: Nach der
   * fachlichen Auskunft des Auftraggebers vom 03.09.2026 wartet eine {@code SUSPENDED}-Nachricht
   * <i>absichtlich</i> — auf einen Folgeprozess, etwa den Versand zu einem bestimmten Zeitpunkt —
   * und wird <b>nie automatisch beendet</b>. Die Frist wird auf sie also nicht angewendet. <b>Ein
   * Feld, das eine Frist nennt, die niemand durchsetzt, nennt eine Zahl statt einer Auskunft.</b>
   *
   * <p><b>Bei {@code RUNNING} bleibt sie und wird erst jetzt richtig:</b> Dort <i>gibt</i> es einen
   * Waechter — laeuft die Frist ab, setzt das Altsystem den Status auf {@code ERROR_TIMEOUT}.
   * Zusammen mit {@code wartetSeitSekunden} sagt das Feld damit, <b>wann die Nachricht kippt</b>.
   *
   * <p><b>Herkunft:</b> fachliche Auskunft des Auftraggebers vom 03.09.2026. <b>Nicht gemessen.</b>
   * Die Testkopie kann sie nicht belegen: {@code RUNNING} kommt dort null Mal vor, und die 538
   * {@code SUSPENDED} sind der Bestand <i>eines</i> Status in <i>einer</i> Gestalt. <b>Gegen die
   * Produktion zu pruefen</b> mit der Abfrage in {@code docs/message-status.md}, Abschnitt „Die
   * offene Pruefung".
   *
   * <p><b>Fuer die uebrigen Einordnungen aendert sich nichts</b>, und das ist eine Entscheidung und
   * keine Auslassung: Bei einer abgeschlossenen Nachricht ist die Frist eine Tatsache ueber die
   * Zeile und keine Zusage ueber die Zukunft. Ob sie auch dort {@code null} werden sollte, ist
   * offener Punkt 132 — <b>in diesem Schritt nicht entschieden</b>.
   *
   * <p><b>{@code 0} und {@code NULL} werden weiterhin beide zu {@code null}</b> — „keine Frist
   * gesetzt". Eine gelieferte {@code 0} liesse sich als „sofort faellig" lesen, und das steht
   * nirgends. <b>Dass die {@code 0} „kein Timeout" bedeutet, ist eine Analogie und kein Befund:</b>
   * Bei allen 52 {@code ERROR_TIMEOUT}-Nachrichten traegt die fehlschlagende Aktion {@code
   * SOSActionTimeout = 0}, und trotzdem greift dort eine Frist von hoechstens 120 Sekunden (M8). In
   * dieser Spalte heisst {@code 0} also eher „nimm die Vorgabe" als „keine Frist". Praktisch
   * folgenlos ist das nur, weil alle 6.915 Zeilen mit {@code MessageTimeout = 0} in einem Endstatus
   * stehen (M2). <b>Taucht in Produktion eine offene Zeile mit {@code 0} auf, ist hier
   * nachzusehen.</b> Die Begruendung stand bis zum 03.09.2026 an {@code
   * MessageStatusClassifier.timeoutZeitpunkt}; jene Methode ist mit E‑71 entfallen, und die
   * Ueberlegung ist hierher gewandert — an die einzige Stelle, an der sie noch wirkt.
   */
  private static Integer frist(NachrichtKopfZeile kopf, MessageStatusKind einordnung) {
    if (einordnung == MessageStatusKind.WARTEND) {
      return null;
    }
    return kopf.timeoutSekunden() == null || kopf.timeoutSekunden() <= 0
        ? null
        : (int) kopf.timeoutSekunden();
  }

  /**
   * Der Abstand zweier Zeitpunkte in ganzen Sekunden, {@code null} bei fehlendem Wert <b>und bei
   * negativem Abstand</b>.
   *
   * <p>Dieselbe Regel wie bei {@link #dauerSekunden(MessageAktion)} und aus demselben Grund:
   * „wartet seit minus drei Sekunden" ist schlechter als gar keine Angabe. Vorgekommen ist es nicht
   * — die Anwendungsuhr im Profil {@code dev} steht auf dem juengsten Zeitpunkt des Bestands und
   * laeuft vorwaerts —, aber eine Uhr, die einmal zurueckspringt, soll keine negative Dauer
   * erzeugen.
   */
  private static Long abstand(LocalDateTime von, LocalDateTime bis) {
    if (von == null || bis == null) {
      return null;
    }
    long sekunden = Duration.between(von, bis).toSeconds();
    return sekunden < 0 ? null : sekunden;
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
