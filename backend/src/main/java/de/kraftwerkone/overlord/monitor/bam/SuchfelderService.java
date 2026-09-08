package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Das Angebot der Suchfläche: <b>beide Quellen, zwei Gruppen</b> (E‑99).
 *
 * <p>Die BAM-Gruppe kommt aus {@link BamTypenService} — <b>wiederverwendet, nicht nachgebaut</b>.
 * Die Feld-Gruppe kommt aus {@link SuchfelderRepository}, gefiltert auf die globalen und die dem
 * Mandanten zugeordneten Einträge. Beide Typen stehen im Angebot (E‑101); was das Backend mit ihnen
 * tut, unterscheidet sich, und genau das sagt {@code spalte} an jedem Eintrag.
 *
 * <h2>Ein Typ‑0‑Name, den die Abbildung nicht kennt, wird nicht angeboten</h2>
 *
 * <p>Für ihn gäbe es kein Spaltenprädikat, und ein Angebot ohne Prädikat wäre ein Feld, das nie
 * etwas findet. Er fällt <b>still</b> aus dem Angebot — und das ist nur deshalb vertretbar, weil
 * {@code Typ0AbbildungDbIT} rot wird, sobald ein solcher Name in der Konfiguration steht. Ohne den
 * Test wäre das Verschwinden unsichtbar; mit ihm ist es ein Bauauftrag mit einer Zeile in {@link
 * Typ0Feld}. Dieselbe Bauform wie die Sicherung gegen neue Statuswerte ({@code
 * PROJEKTBESCHREIBUNG.md} §4.1).
 *
 * <p><b>Dasselbe gilt für einen Eintrag ohne bekannten Typ</b> — {@code NULL} oder ein dritter
 * Wert. M154 kennt genau {@code 0} und {@code 1}; für alles andere steht weder Spaltenprädikat noch
 * EAV-Zugriff fest, und Raten ist ausgeschlossen (Regel Q4). Auch das meldet der Test.
 *
 * <p><b>Keine Beschriftung, keine Übersetzung, keine Aussiebung nach Deckung</b> (E‑105). {@code
 * Message.DestinationFilename} und {@code Message.ReceiverID} stehen mit 14,5 und 12,8 Prozent
 * Deckung kommentarlos im Angebot — der Schutz gegen ein schlechtes Feld ist strukturell
 * (Pflichtfenster, Deckelung) und keine Namensliste.
 */
@Service
public class SuchfelderService {

  private final BamTypenService bamTypenService;
  private final SuchfelderRepository suchfelderRepository;

  SuchfelderService(BamTypenService bamTypenService, SuchfelderRepository suchfelderRepository) {
    this.bamTypenService = bamTypenService;
    this.suchfelderRepository = suchfelderRepository;
  }

  /** Das Angebot des aktiven Mandanten, beide Gruppen. */
  public SuchfelderResponse angebot(MandantContext mandant) {
    List<SuchfeldBamResponse> bam =
        bamTypenService.typen(mandant).stream()
            .map(
                typ ->
                    new SuchfeldBamResponse(
                        SuchfelderResponse.QUELLE_BAM,
                        typ.typ(),
                        typ.bezeichnung(),
                        typ.sortIndex()))
            .toList();

    List<SuchfeldFeldResponse> felder = new ArrayList<>();
    for (SuchfeldZeile zeile : suchfelderRepository.findeKonfigurierteFelder(mandant)) {
      angebotFuer(zeile).ifPresent(felder::add);
    }
    return new SuchfelderResponse(bam, felder);
  }

  /**
   * Der Angebotseintrag zu einer Konfigurationszeile — oder leer, wenn die Zeile nicht anbietbar
   * ist: ein Typ‑0‑Name ohne Abbildung, ein unbekannter Typ.
   */
  static Optional<SuchfeldFeldResponse> angebotFuer(SuchfeldZeile zeile) {
    if (zeile.typ() == null) {
      return Optional.empty();
    }
    int typ = zeile.typ();
    return switch (typ) {
      case 0 ->
          Typ0Feld.fuer(zeile.name())
              .map(
                  feld ->
                      new SuchfeldFeldResponse(SuchfelderResponse.QUELLE_FELD, zeile.name(), true));
      case 1 ->
          Optional.of(
              new SuchfeldFeldResponse(SuchfelderResponse.QUELLE_FELD, zeile.name(), false));
      default -> Optional.empty();
    };
  }
}
