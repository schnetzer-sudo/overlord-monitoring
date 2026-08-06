package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.BamSpalte;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Seite;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Die Fachlogik der Nachrichtenliste: Suchbegriff aufloesen, Seite lesen, BAM-Werte nachladen,
 * Rohwerte einordnen.
 *
 * <p>Die Reihenfolge ist kein Zufall. Erst wird der Suchbegriff gegen die <b>Stammdaten</b>
 * aufgeloest (1.490 Prozesse, 1.818 Ablaeufe); trifft er nichts, ist die Liste leer, ohne dass
 * {@code Message} ueberhaupt angefasst wird. Dann die eine paginierte Abfrage. Erst danach — und
 * nur fuer die tatsaechlich gelieferten Zeilen — die BAM-Werte.
 */
@Service
public class NachrichtenService {

  /**
   * Wie viele Werte einer BAM-Spalte angezeigt werden. Drei, weil eine Listenzelle sonst die Zeile
   * sprengt; was darueber liegt, wird gezaehlt und nicht verschwiegen.
   */
  public static final int BAM_WERTE_JE_SPALTE = 3;

  private final NachrichtenRepository nachrichtenRepository;
  private final BamSpaltenRepository bamSpaltenRepository;
  private final MessageStatusClassifier statusClassifier;
  private final Clock anwendungsuhr;

  NachrichtenService(
      NachrichtenRepository nachrichtenRepository,
      BamSpaltenRepository bamSpaltenRepository,
      MessageStatusClassifier statusClassifier,
      Clock anwendungsuhr) {
    this.nachrichtenRepository = nachrichtenRepository;
    this.bamSpaltenRepository = bamSpaltenRepository;
    this.statusClassifier = statusClassifier;
    this.anwendungsuhr = anwendungsuhr;
  }

  /** Eine Seite der Liste fuer den aktiven Mandanten. */
  public Seite<NachrichtResponse> liste(MandantContext mandant, NachrichtenFilter filter) {
    Suchtreffer suchtreffer = suchtreffer(mandant, filter);
    if (suchtreffer != null && suchtreffer.leer()) {
      return new Seite<>(List.of(), null, false);
    }

    List<NachrichtZeile> gelesen =
        nachrichtenRepository.finde(mandant, Nachrichtenabfrage.aus(filter, suchtreffer));
    Seite<NachrichtZeile> seite =
        Seite.aus(
            gelesen,
            filter.limit(),
            zeile -> new Seitenposition(zeile.zeitpunkt(), zeile.messageId()));

    return new Seite<>(uebersetze(mandant, seite.items()), seite.nextCursor(), seite.hasMore());
  }

  private Suchtreffer suchtreffer(MandantContext mandant, NachrichtenFilter filter) {
    if (filter.suche() == null) {
      return null;
    }
    Suchtreffer treffer =
        nachrichtenRepository.loeseSucheAuf(
            mandant, filter.suche(), NachrichtenFilter.SUCHE_HOECHSTENS_TREFFER);
    if (treffer.zuUnscharf()) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "suchbegriff-zu-unscharf",
          "Suchbegriff zu unscharf",
          "Der Suchbegriff trifft zu viele Prozesse. Verenge ihn.",
          "Suchbegriff ueber der Trefferobergrenze");
    }
    return treffer;
  }

  /** Rohwerte zu Antwortzeilen: Einordnung, UTC-Zeitpunkt, BAM-Werte. */
  private List<NachrichtResponse> uebersetze(MandantContext mandant, List<NachrichtZeile> zeilen) {
    if (zeilen.isEmpty()) {
      return List.of();
    }
    List<BamSpalte> spalten = bamSpaltenRepository.spalten(mandant);
    Map<String, Map<Short, List<String>>> bamWerte = bamWerte(mandant, zeilen, spalten);

    List<NachrichtResponse> antwort = new ArrayList<>(zeilen.size());
    for (NachrichtZeile zeile : zeilen) {
      MessageStatusKind einordnung = statusClassifier.einordnung(zeile.status());
      antwort.add(
          new NachrichtResponse(
              zeile.messageId(),
              Zeitpunkte.nachUtc(zeile.zeitpunkt(), anwendungsuhr.getZone()),
              zeile.status(),
              einordnung.name(),
              einordnung == MessageStatusKind.UNGEKLAERT,
              zeile.processId(),
              zeile.processName(),
              zeile.projectName(),
              spaltenWerte(spalten, bamWerte.getOrDefault(zeile.messageId(), Map.of()))));
    }
    return List.copyOf(antwort);
  }

  /**
   * Laedt die BAM-Werte der Seite in <b>einer</b> zweiten Abfrage und gruppiert sie im Speicher.
   *
   * <p>Erst hier, und nur fuer die tatsaechlich gelieferten Zeilen: Die Zusatzzeile, an der die
   * Seite „gibt es eine naechste" erkennt, ist zu diesem Zeitpunkt schon abgeschnitten.
   *
   * <p>Hat der Mandant keine BAM-Konfiguration ({@code EDITIONLINGERI}, {@code SYSTEM}, {@code
   * WOC}), entfaellt die Abfrage vollstaendig — kein Aufruf mit leerer Typliste.
   */
  private Map<String, Map<Short, List<String>>> bamWerte(
      MandantContext mandant, List<NachrichtZeile> zeilen, List<BamSpalte> spalten) {
    if (spalten.isEmpty()) {
      return Map.of();
    }
    List<Short> typen = spalten.stream().map(BamSpalte::typ).toList();
    List<String> messageIds = zeilen.stream().map(NachrichtZeile::messageId).toList();

    Map<String, Map<Short, List<String>>> gruppiert = new HashMap<>();
    for (BamWert wert : nachrichtenRepository.findeBamWerte(mandant, messageIds, typen)) {
      gruppiert
          .computeIfAbsent(wert.messageId(), id -> new HashMap<>())
          .computeIfAbsent(wert.typ(), typ -> new ArrayList<>())
          .add(wert.wert());
    }
    return gruppiert;
  }

  /**
   * Die Werte einer Zeile, in der Reihenfolge der Spalten — auch dann, wenn eine Spalte fuer diese
   * Nachricht leer bleibt. Sonst muesste die Oberflaeche die Spalten je Zeile neu ausrichten.
   *
   * <p>Sortiert wird <b>im Speicher</b>, nicht in SQL: Ein {@code ORDER BY MessageBAMValue} waere
   * eine Sortierung ueber genau die Spalte, die Regel L4/L5 dafuer ausschliesst. Hier geht es um
   * hoechstens ein paar hundert Zeichenketten.
   */
  private static List<NachrichtResponse.BamWerte> spaltenWerte(
      List<BamSpalte> spalten, Map<Short, List<String>> werteJeTyp) {
    List<NachrichtResponse.BamWerte> ergebnis = new ArrayList<>(spalten.size());
    for (BamSpalte spalte : spalten) {
      List<String> alle = sortiert(werteJeTyp.get(spalte.typ()));
      ergebnis.add(
          new NachrichtResponse.BamWerte(
              spalte.typ(),
              spalte.beschreibung(),
              List.copyOf(alle.subList(0, Math.min(alle.size(), BAM_WERTE_JE_SPALTE))),
              Math.max(0, alle.size() - BAM_WERTE_JE_SPALTE)));
    }
    return List.copyOf(ergebnis);
  }

  /** Doppelte Werte fallen weg — derselbe Lieferschein zweimal ist keine zusaetzliche Auskunft. */
  private static List<String> sortiert(List<String> werte) {
    return werte == null ? List.of() : werte.stream().distinct().sorted().toList();
  }
}
