package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Seite;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Die Fachlogik der Nachrichtenliste: Suchbegriff aufloesen, Seite lesen, Rohwerte einordnen.
 *
 * <p>Die Reihenfolge ist kein Zufall. Erst wird der Suchbegriff gegen die <b>Stammdaten</b>
 * aufgeloest (1.490 Prozesse, 1.818 Ablaeufe); trifft er nichts, ist die Liste leer, ohne dass
 * {@code Message} ueberhaupt angefasst wird. Dann die eine paginierte Abfrage — und das ist seit
 * der Nachbesserung zu Schritt 4 auch die einzige.
 *
 * <p><b>Das Nachladen der BAM-Werte ist entfallen.</b> Es war die zweite Abfrage je Seite und
 * fuellte zwei Spalten, die Messung M11 als praktisch leer ausweist: 98,93 und 96,97 Prozent der
 * Zeilen ohne Wert. Der Anzeigename des Ablaufs steht stattdessen im Hauptstatement (L14, {@code
 * eq_ref}, 0,2 ms) — eine Spalte, die auf jeder Zeile etwas zeigt, statt zweier, die es fast nie
 * tun.
 */
@Service
public class NachrichtenService {

  private static final Logger log = LoggerFactory.getLogger(NachrichtenService.class);

  /**
   * Wie lange die Merkmale eines Mandanten gehalten werden.
   *
   * <p>Eine Stunde ist der Ausgleich zwischen zwei Fehlern: Zu kurz gehalten wiederholt sich ein
   * Statement, das im schlimmsten Fall den ganzen Bestand eines Mandanten liest (L15, 331 ms); zu
   * lang gehalten erscheint der Schalter beim ersten Zwischenschritt eines Mandanten erst nach
   * einer halben Ewigkeit. Der Wert beschreibt den Gesamtbestand und aendert sich, wenn ueberhaupt,
   * genau einmal — von {@code false} auf {@code true}.
   */
  private static final Duration MERKMALE_HALTBARKEIT = Duration.ofHours(1);

  private final NachrichtenRepository nachrichtenRepository;
  private final MessageStatusClassifier statusClassifier;
  private final Clock anwendungsuhr;
  private final Clock systemClock;

  /**
   * Der Zwischenspeicher der Merkmale, je Mandant.
   *
   * <p>Bewusst eine {@link ConcurrentHashMap} und keine Cache-Abstraktion: Es geht um einen
   * Wahrheitswert je Mandant, bei hoechstens zehn Mandanten. Ein {@code CacheManager} waere mehr
   * Verdrahtung als Gegenwert — und eine Tabelle waere eine Konfiguration, die gepflegt werden
   * muesste, statt ermittelt zu werden.
   */
  private final Map<String, Merkmalstand> merkmaleJeMandant = new ConcurrentHashMap<>();

  private record Merkmalstand(boolean zwischenschritteVorhanden, Instant ermitteltAm) {}

  NachrichtenService(
      NachrichtenRepository nachrichtenRepository,
      MessageStatusClassifier statusClassifier,
      Clock anwendungsuhr,
      @Qualifier("systemClock") Clock systemClock) {
    this.nachrichtenRepository = nachrichtenRepository;
    this.statusClassifier = statusClassifier;
    this.anwendungsuhr = anwendungsuhr;
    this.systemClock = systemClock;
  }

  /**
   * Die Merkmale des Bestands fuer den aktiven Mandanten — <b>zwischengespeichert</b>.
   *
   * <p><b>Die Haltbarkeit rechnet gegen die Systemuhr, nicht gegen die Anwendungsuhr</b> (Regel A5
   * sinngemaess): Die Anwendungsuhr ist im Profil {@code dev} um Monate zurueckversetzt, und ein
   * Zwischenspeicher, der gegen eine stehende Uhr rechnet, laeuft nie ab. Hier geht es um
   * <i>technische</i> Zeit — wie alt ist dieser Wert —, nicht um fachliche.
   */
  public NachrichtenMerkmaleResponse merkmale(MandantContext mandant) {
    Instant jetzt = systemClock.instant();
    Merkmalstand stand = merkmaleJeMandant.get(mandant.mandantId());
    if (stand == null || stand.ermitteltAm().plus(MERKMALE_HALTBARKEIT).isBefore(jetzt)) {
      stand = new Merkmalstand(ermittleZwischenschritte(mandant), jetzt);
      merkmaleJeMandant.put(mandant.mandantId(), stand);
    }
    return new NachrichtenMerkmaleResponse(stand.zwischenschritteVorhanden());
  }

  /**
   * <b>Scheitert die Ermittlung, wird der Schalter gezeigt.</b>
   *
   * <p>Das ist der Zustand, den die Liste bis heute hatte: ein Schalter fuer alle. Ein Fehler beim
   * Ermitteln eines <i>Anzeigehinweises</i> darf die Liste nicht mitreissen — die Nachricht, die
   * der Nutzer sucht, findet er auch ohne diese Auskunft.
   *
   * <p><b>Auch der Rueckfall wird zwischengespeichert.</b> Sonst liefe das Statement bei einem
   * dauerhaft scheiternden Mandanten bei <i>jedem</i> Aufruf erneut in dieselbe Zeitgrenze — und
   * damit waere der Schutzmechanismus die Last. Sichtbar bleibt es im Protokoll: Der Eintrag steht
   * auf {@code WARN} und traegt die Ausnahme.
   */
  private boolean ermittleZwischenschritte(MandantContext mandant) {
    try {
      return nachrichtenRepository.hatZwischenschritte(mandant);
    } catch (DataAccessException fehler) {
      log.warn(
          "Zwischenschritte konnten nicht ermittelt werden; der Schalter bleibt sichtbar.", fehler);
      return true;
    }
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

    return new Seite<>(uebersetze(seite.items()), seite.nextCursor(), seite.hasMore());
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

  /** Rohwerte zu Antwortzeilen: Einordnung und UTC-Zeitpunkt. */
  private List<NachrichtResponse> uebersetze(List<NachrichtZeile> zeilen) {
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
              zeile.sosName()));
    }
    return List.copyOf(antwort);
  }
}
