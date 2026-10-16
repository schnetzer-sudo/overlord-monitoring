package de.kraftwerkone.overlord.monitor.assistent;

import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeug;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Die laufenden Gespraeche — <b>nur im Arbeitsspeicher</b> (E‑1038): nicht in der Datenbank, nicht
 * in der Sitzung, nicht im Log. Ein Neustart beendet alle.
 *
 * <p><b>Ein Gespraech gehoert genau einer Sitzung, einem Nutzer und einem Mandanten.</b> {@link
 * #finde} verlangt alle drei; ein fremdes Gespraech — auch eines desselben Nutzers aus einer
 * anderen Sitzung — ist dasselbe wie ein unbekanntes. Beim Wechsel des aktiven Mandanten endet
 * jedes Gespraech dieser Sitzung ({@link #beendeNachMandantenwechsel}).
 *
 * <p><b>Ablauf und Hoechstzahl:</b> 30 Minuten ohne Nachricht, dann ist es weg; ein viertes
 * Gespraech derselben Sitzung beendet das am laengsten ruhende (E‑1053). Gerechnet wird mit der
 * Systemuhr, nicht mit der Anwendungsuhr (E‑1057).
 */
@Component
public class Gespraechsspeicher {

  private final ConcurrentMap<UUID, Gespraech> gespraeche = new ConcurrentHashMap<>();
  private final Clock uhr;
  private final Duration ablauf;
  private final int jeSitzung;

  @Autowired
  Gespraechsspeicher(@Qualifier("systemClock") Clock uhr, AssistentEigenschaften eigenschaften) {
    this(uhr, eigenschaften.gespraechAblauf(), eigenschaften.gespraecheJeSitzung());
  }

  Gespraechsspeicher(Clock uhr, Duration ablauf, int jeSitzung) {
    this.uhr = uhr;
    this.ablauf = ablauf;
    this.jeSitzung = jeSitzung;
  }

  /** Eroeffnet ein Gespraech mit festgelegter Anweisung und festgelegten Werkzeugen (E‑1045). */
  public synchronized Gespraech eroeffne(
      MandantContext mandant,
      long nutzerId,
      String sitzungId,
      String anweisung,
      List<Werkzeug> werkzeuge) {
    raeumeAbgelaufeneAuf();
    beendeNachMandantenwechsel(mandant, sitzungId);
    List<Gespraech> derSitzung =
        gespraeche.values().stream()
            .filter(gespraech -> gespraech.sitzungId().equals(sitzungId))
            .sorted(Comparator.comparing(Gespraech::letzteAktivitaet))
            .toList();
    for (int i = 0; i <= derSitzung.size() - jeSitzung; i++) {
      gespraeche.remove(derSitzung.get(i).id());
    }
    Gespraech neu =
        new Gespraech(
            UUID.randomUUID(),
            sitzungId,
            nutzerId,
            mandant.mandantId(),
            anweisung,
            werkzeuge,
            uhr.instant());
    gespraeche.put(neu.id(), neu);
    return neu;
  }

  /**
   * Das Gespraech, wenn es dieser Sitzung, diesem Nutzer und diesem Mandanten gehoert und nicht
   * abgelaufen ist. Sonst leer — in allen Faellen gleich.
   */
  public Optional<Gespraech> finde(
      MandantContext mandant, long nutzerId, String sitzungId, UUID id) {
    Gespraech gespraech = gespraeche.get(id);
    if (gespraech == null) {
      return Optional.empty();
    }
    if (abgelaufen(gespraech)) {
      gespraeche.remove(id, gespraech);
      return Optional.empty();
    }
    if (!gespraech.sitzungId().equals(sitzungId)) {
      return Optional.empty();
    }
    if (gespraech.nutzerId() != nutzerId) {
      return Optional.empty();
    }
    if (!gespraech.mandantId().equals(mandant.mandantId())) {
      return Optional.empty();
    }
    return Optional.of(gespraech);
  }

  /** Beendet das Gespraech. {@code false}, wenn {@link #finde} es nicht findet. */
  public boolean beende(MandantContext mandant, long nutzerId, String sitzungId, UUID id) {
    return finde(mandant, nutzerId, sitzungId, id)
        .map(gespraech -> gespraeche.remove(id, gespraech))
        .orElse(false);
  }

  /**
   * Beendet jedes Gespraech dieser Sitzung, das an einem anderen als dem aktiven Mandanten haengt
   * (E‑1038). Laeuft bei jedem Zugriff auf den Assistenten, also spaetestens beim ersten Zugriff
   * nach dem Wechsel.
   */
  public void beendeNachMandantenwechsel(MandantContext mandant, String sitzungId) {
    gespraeche
        .values()
        .removeIf(
            gespraech ->
                gespraech.sitzungId().equals(sitzungId)
                    && !gespraech.mandantId().equals(mandant.mandantId()));
  }

  /** Die Zahl der gehaltenen Gespraeche, abgelaufene eingeschlossen — fuer Tests. */
  int anzahl() {
    return gespraeche.size();
  }

  private void raeumeAbgelaufeneAuf() {
    gespraeche.values().removeIf(this::abgelaufen);
  }

  private boolean abgelaufen(Gespraech gespraech) {
    Instant grenze = gespraech.letzteAktivitaet().plus(ablauf);
    return !uhr.instant().isBefore(grenze);
  }
}
