package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Seite;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
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

  private final NachrichtenRepository nachrichtenRepository;
  private final MessageStatusClassifier statusClassifier;
  private final Clock anwendungsuhr;

  NachrichtenService(
      NachrichtenRepository nachrichtenRepository,
      MessageStatusClassifier statusClassifier,
      Clock anwendungsuhr) {
    this.nachrichtenRepository = nachrichtenRepository;
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
