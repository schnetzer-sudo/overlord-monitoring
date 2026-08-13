package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.common.Kettenrolle;
import de.kraftwerkone.overlord.monitor.common.Kettenrollen;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Die Fachlogik der BAM-Suche: <b>normalisieren, suchen, beschriften</b>.
 *
 * <p>Die Reihenfolge ist kein Zufall. Zuerst wird die <b>Kuratierung des Mandanten</b> gelesen und
 * daraus je Begriff die Variantenmenge gebildet — <i>vor</i> dem Statement, weil {@code
 * bam_sollaenge} nie gegen {@code GlassfishDB} gejoint wird. Dann die eine Abfrage, die über die
 * Menge entscheidet. Und erst danach die zweite, die für die höchstens fünfzig gefundenen
 * Nachrichten nachschlägt, <b>worauf</b> getroffen wurde.
 *
 * <p><b>Ein zusätzlicher Zugriff je Suche, und er ist winzig.</b> Der Vollabzug der Kuratierung
 * liest bei keinem Mandanten mehr als elf Zeilen aus einer Tabelle von sechzehn.
 */
@Service
public class BamSucheService {

  private final BamSucheRepository bamSucheRepository;
  private final MessageStatusClassifier statusClassifier;
  private final Clock anwendungsuhr;

  BamSucheService(
      BamSucheRepository bamSucheRepository,
      MessageStatusClassifier statusClassifier,
      Clock anwendungsuhr) {
    this.bamSucheRepository = bamSucheRepository;
    this.statusClassifier = statusClassifier;
    this.anwendungsuhr = anwendungsuhr;
  }

  /** Die Trefferliste für den aktiven Mandanten. */
  public BamSucheResponse suche(MandantContext mandant, BamSuchfilter filter) {
    Sollaengen sollaengen = new Sollaengen(bamSucheRepository.findeSollaengen(mandant));

    List<Varianten> varianten = new ArrayList<>(filter.begriffe().size());
    List<Suchbedingung> bedingungen = new ArrayList<>(filter.begriffe().size());
    for (Suchbegriff begriff : filter.begriffe()) {
      Varianten fassungen = sollaengen.fuer(begriff);
      varianten.add(fassungen);
      bedingungen.add(new Suchbedingung(begriff.typ(), fassungen.gesucht()));
    }

    List<BamTrefferZeile> gelesen =
        bamSucheRepository.findeTreffer(mandant, bedingungen, filter.fenster());

    // Die Abschneidung wird an der (n+1)-ten Zeile erkannt — und damit NACH dem Mandantenfilter,
    // weil der im Statement steht. Eine Zahl aus den Rohtreffern beschriebe fremden Bestand.
    boolean abgeschnitten = gelesen.size() > BamSucheRepository.HOECHSTENS_TREFFER;
    List<BamTrefferZeile> zeilen =
        abgeschnitten ? gelesen.subList(0, BamSucheRepository.HOECHSTENS_TREFFER) : gelesen;

    Map<String, List<BamTrefferWertResponse>> trefferJeNachricht =
        trefferJeNachricht(
            bamSucheRepository.findeTrefferWerte(mandant, kennungen(zeilen), bedingungen));

    return new BamSucheResponse(
        uebersetze(zeilen, trefferJeNachricht),
        begriffe(filter.begriffe(), varianten),
        Zeitpunkte.nachUtc(filter.fenster().von(), anwendungsuhr.getZone()),
        Zeitpunkte.nachUtc(filter.fenster().bis(), anwendungsuhr.getZone()),
        abgeschnitten);
  }

  private static List<String> kennungen(List<BamTrefferZeile> zeilen) {
    return zeilen.stream().map(BamTrefferZeile::messageId).toList();
  }

  /**
   * Die Treffer, nach Nachricht aufgeteilt.
   *
   * <p>Die Reihenfolge innerhalb einer Nachricht bleibt die der Abfrage — aufsteigend nach Typ und
   * Wert. Hier wird <b>nicht ein zweites Mal sortiert</b>: Eine zweite Ordnung an einer zweiten
   * Stelle ist genau die Drift, bei der zwei Aufrufe dasselbe verschieden zeigen.
   */
  private static Map<String, List<BamTrefferWertResponse>> trefferJeNachricht(
      List<BamTrefferWertZeile> zeilen) {
    Map<String, List<BamTrefferWertResponse>> treffer = new LinkedHashMap<>();
    for (BamTrefferWertZeile zeile : zeilen) {
      treffer
          .computeIfAbsent(zeile.messageId(), kennung -> new ArrayList<>())
          .add(
              new BamTrefferWertResponse(
                  zeile.typ(),
                  Typbezeichnung.fuer(zeile.typ(), zeile.bezeichnung()),
                  zeile.wert()));
    }
    return treffer;
  }

  /**
   * Rohwerte zu Antwortzeilen: Einordnung, UTC-Zeitpunkt, Rollen und der Schritt, sofern er einer
   * ist.
   */
  private List<BamTrefferResponse> uebersetze(
      List<BamTrefferZeile> zeilen, Map<String, List<BamTrefferWertResponse>> trefferJeNachricht) {
    List<BamTrefferResponse> antwort = new ArrayList<>(zeilen.size());
    for (BamTrefferZeile zeile : zeilen) {
      MessageStatusKind einordnung = statusClassifier.einordnung(zeile.status());
      List<Kettenrolle> rollen =
          List.copyOf(
              Kettenrollen.aus(
                  zeile.source(),
                  zeile.sourceMessageId(),
                  zeile.targetMessageId(),
                  zeile.target()));
      antwort.add(
          new BamTrefferResponse(
              zeile.messageId(),
              Zeitpunkte.nachUtc(zeile.zeitpunkt(), anwendungsuhr.getZone()),
              zeile.status(),
              einordnung.name(),
              einordnung == MessageStatusKind.UNGEKLAERT,
              zeile.processId(),
              zeile.processName(),
              zeile.projectName(),
              zeile.sosName(),
              aktuellerSchritt(einordnung, zeile.schritt()),
              rollen,
              trefferJeNachricht.getOrDefault(zeile.messageId(), List.of())));
    }
    return List.copyOf(antwort);
  }

  /**
   * Die Begriffe mit ihren gemeldeten Fassungen — <b>ohne die mit führendem Leerzeichen</b> ({@link
   * Varianten#gemeldet()}).
   */
  private static List<BamBegriffResponse> begriffe(
      List<Suchbegriff> begriffe, List<Varianten> varianten) {
    List<BamBegriffResponse> antwort = new ArrayList<>(begriffe.size());
    for (int i = 0; i < begriffe.size(); i++) {
      Suchbegriff begriff = begriffe.get(i);
      antwort.add(
          new BamBegriffResponse(begriff.wert(), begriff.typ(), varianten.get(i).gemeldet()));
    }
    return List.copyOf(antwort);
  }

  /**
   * Der Schritt, auf dem eine Nachricht <b>gerade steht</b> — und {@code null}, wenn sie auf keinem
   * mehr steht.
   *
   * <p>Dieselbe fachliche Entscheidung wie in der Nachrichtenliste, und sie wird hier
   * <b>ausdrücklich aufgezählt und nicht über {@code MessageStatusClassifier.istEndstatus}
   * geholt</b>: Jene Methode gehört der Überfälligkeitsrechnung und antwortet für {@code
   * UNGEKLAERT} mit {@code true} — in ihrem Zusammenhang die vorsichtige Antwort, hier wäre
   * dieselbe {@code true} die unvorsichtige ({@code docs/message-status.md}).
   */
  private static String aktuellerSchritt(MessageStatusKind einordnung, String schritt) {
    return einordnung == MessageStatusKind.WARTEND || einordnung == MessageStatusKind.LAEUFT
        ? schritt
        : null;
  }
}
