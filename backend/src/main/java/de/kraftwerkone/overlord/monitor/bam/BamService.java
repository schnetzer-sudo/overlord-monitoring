package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Die Fachlogik des BAM-Blocks: <b>zaehlen, deckeln, ordnen</b>.
 *
 * <p>Was hier <b>nicht</b> geschieht: filtern. Die Konfiguration aus {@code MessageBAMMandant}
 * ordnet die Gruppen und siebt keine aus — die Begruendung steht bei {@link #ORDNUNG}.
 */
@Service
public class BamService {

  /**
   * Die Ordnung der Gruppen: <b>zuerst die konfigurierten Typen nach ihrem {@code
   * MessageBAMTypeSortIndex}, danach alle uebrigen nach Typnummer.</b>
   *
   * <p><b>Die Konfiguration ist Ordnung und kein Sieb</b> — entschieden im Sparring vom 12.08.2026
   * auf Grundlage von M40, und aus zwei Gruenden:
   *
   * <ol>
   *   <li><b>Sie sagt nichts ueber den Bestand.</b> {@code WOC} hat <i>keinen</i> konfigurierten
   *       Typ und traegt trotzdem 2.067 BAM-Zeilen unter Typ 9014. Folgte der Block der
   *       Konfiguration als Filter, saehe dieser Mandant <b>nichts</b>. Bei {@code SYSTEM} deckten
   *       sich Konfiguration und Bestand, bei {@code WOC} nicht — die Konfiguration ist damit
   *       gemessen eine <i>Sichtbarkeits</i>entscheidung.
   *   <li><b>Der schwerere Grund kommt aus Teil 3.</b> Die Suche ist typlos (M32, M36) und findet
   *       also Werte unter unkonfigurierten Typen. Faende ein Nutzer eine Nachricht und saehe im
   *       Block den gesuchten Wert nicht, waere das ein Widerspruch, den niemand aufloesen kann.
   * </ol>
   *
   * <p><b>Ein unkonfigurierter Typ wird nicht markiert.</b> Ob ein Typ in {@code MessageBAMMandant}
   * steht, ist eine interne Angabe und geht den Nutzer nichts an; er sieht ihn schlicht weiter
   * unten stehen. Genau deshalb verlaesst {@code sortIndex} das Backend nicht.
   *
   * <p><b>Sie liegt hier und nicht im {@code ORDER BY}</b>, damit sie ohne Datenbank pruefbar ist —
   * die beiden Faelle, die sie tragen (Reihenfolge der konfigurierten Typen, unkonfigurierter Typ
   * hinten), sind Entscheidungen und keine Zugriffspfade. Die Menge ist von Natur aus klein:
   * hoechstens 18 Gruppen je Nachricht (M41).
   */
  static final Comparator<BamTypZeile> ORDNUNG =
      Comparator.comparing((BamTypZeile zeile) -> zeile.sortIndex() == null)
          .thenComparing(BamTypZeile::sortIndex, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(BamTypZeile::typ);

  private final BamRepository bamRepository;

  BamService(BamRepository bamRepository) {
    this.bamRepository = bamRepository;
  }

  /**
   * Die Belegnummern einer Nachricht, nach Typ gruppiert und je Gruppe gedeckelt.
   *
   * <p><b>Die Existenz wird zuerst geprueft</b> und nicht aus der Zeilenzahl geschlossen: 80,6
   * Prozent aller Nachrichten tragen keinen BAM-Wert (M41), eine leere Antwort ist also der
   * Normalfall — und darf gerade deshalb nicht dasselbe heissen wie „gibt es nicht".
   *
   * <p><b>Ohne Typen entfaellt der zweite Zugriff.</b> Es gibt nichts zu deckeln, wo nichts steht.
   *
   * @throws RessourceNichtGefundenException {@code 404} — sowohl fuer eine erfundene {@code
   *     MessageID} als auch fuer die eines fremden Mandanten, mit <b>derselben</b> Antwort. Beide
   *     Faelle sind dasselbe Statement mit null Zeilen; unterschieden sie sich, liesse sich der
   *     Bestand abfragen
   */
  public BamResponse werte(MandantContext mandant, String messageId) {
    if (!bamRepository.existiert(mandant, messageId)) {
      throw new RessourceNichtGefundenException("Nachricht nicht sichtbar oder nicht vorhanden");
    }

    List<BamTypZeile> typen = bamRepository.zaehleJeTyp(mandant, messageId);
    if (typen.isEmpty()) {
      return new BamResponse(messageId, List.of());
    }

    Map<Short, List<String>> werteJeTyp = werteJeTyp(bamRepository.findeWerte(mandant, messageId));

    List<BamGruppeResponse> gruppen =
        typen.stream().sorted(ORDNUNG).map(zeile -> gruppe(zeile, werteJeTyp)).toList();
    return new BamResponse(messageId, gruppen);
  }

  /**
   * Die gedeckelten Werte, nach Typ aufgeteilt.
   *
   * <p>Die Reihenfolge innerhalb einer Gruppe bleibt die der Abfrage — aufsteigend nach {@code
   * MessageBAMValue}. Hier wird <b>nicht ein zweites Mal sortiert</b>: Eine zweite Ordnung an einer
   * zweiten Stelle ist genau die Drift, bei der zwei Aufrufe dasselbe verschieden zeigen.
   */
  private static Map<Short, List<String>> werteJeTyp(List<BamWertZeile> zeilen) {
    Map<Short, List<String>> werte = new HashMap<>();
    for (BamWertZeile zeile : zeilen) {
      werte.computeIfAbsent(zeile.typ(), typ -> new ArrayList<>()).add(zeile.wert());
    }
    return werte;
  }

  /**
   * Eine Gruppe aus Zaehlung und Werten.
   *
   * <p><b>{@code weitereVorhanden} vergleicht die wahre Zahl mit der gelieferten</b> und nicht mit
   * der Deckelung. Der Unterschied faellt auf, sobald jemand {@link BamRepository#WERTE_JE_GRUPPE}
   * aendert: Ein Vergleich gegen die Konstante wuerde bei einer Gruppe, die genau die Grenze
   * erreicht, „es gibt mehr" behaupten, obwohl alles dasteht.
   */
  private static BamGruppeResponse gruppe(BamTypZeile zeile, Map<Short, List<String>> werteJeTyp) {
    List<String> werte = werteJeTyp.getOrDefault(zeile.typ(), List.of());
    return new BamGruppeResponse(
        zeile.typ(),
        bezeichnung(zeile),
        zeile.gesamt(),
        List.copyOf(werte),
        zeile.gesamt() > werte.size());
  }

  /**
   * Die Beschriftung — {@code MessageBAMTypeDescription}, unveraendert.
   *
   * <p><b>Fehlt sie, erscheint die Typnummer</b>, sichtbar unfertig. Dieselbe Regel wie bei einem
   * kuratierten Eigenschaftsnamen ohne Uebersetzung ({@code nachrichtendetail.md} §10.3): Ein neuer
   * Typ aus dem Altsystem faellt beim ersten Blick auf, statt lautlos als leere Zeile zu
   * erscheinen.
   *
   * <p>Ein <b>leerer</b> Text wird wie ein fehlender behandelt. Die Spalte laesst ihn zu, und eine
   * Gruppe ohne jede Ueberschrift waere die eine Darstellung, die schlechter ist als die Typnummer.
   */
  private static String bezeichnung(BamTypZeile zeile) {
    String beschreibung = zeile.bezeichnung();
    if (beschreibung == null || beschreibung.isBlank()) {
      return Short.toString(zeile.typ());
    }
    return beschreibung;
  }
}
