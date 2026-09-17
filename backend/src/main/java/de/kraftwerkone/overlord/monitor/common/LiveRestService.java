package de.kraftwerkone.overlord.monitor.common;

import java.time.LocalDateTime;
import java.util.List;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <b>Der Baustein</b>: Wasserstand lesen, entscheiden, bei {@code ANGEWANDT} den Live-Bereich lesen
 * und verrechnen ({@code docs/live-rest.md}). Prozessbaum und Dashboard rufen ihn, ohne einander zu
 * kennen.
 *
 * <h2>Ein Uhrenschlag je Anfrage</h2>
 *
 * <p>{@code jetzt} kommt vom Verbraucher — er schlaegt die Anwendungsuhr genau einmal und reicht
 * den Wert an sein Fenster <b>und</b> hierher. Zwei Schlaege waeren zwei Stichtage, und der
 * Live-Bereich koennte in einer anderen Stunde enden als das Fenster, das ihn verrechnet.
 *
 * <h2>Faellt die Live-Lesung aus, ist der Rest ausgesetzt — mit G</h2>
 *
 * <p>Entschieden vom Auftraggeber am 17.09.2026 (Punkt 188): Laeuft eine der beiden Lesungen in
 * einen Datenbankfehler — etwa die Zeitgrenze des Lese-Pools —, antwortet der Verbraucher mit den
 * Rollup-Zahlen, der Zustand ist {@code AUSGESETZT} mit {@code vollstaendigBis} = G, und der Fehler
 * steht als {@code WARN} im Protokoll. Dieselbe Haltung wie die Fensterverengung: Rueckfall statt
 * Ausfall — aber sichtbar, weil die Oberflaeche bei {@code AUSGESETZT} sagt, dass die Zahlen
 * unvollstaendig sind. <b>Die Wasserstandsabfrage selbst wird nicht abgefangen:</b> Sie liest das
 * eigene Schema; faellt sie, ist das kein Rest, der fehlt, sondern ein Vorfall.
 */
@Service
public class LiveRestService {

  private static final Logger LOG = LoggerFactory.getLogger(LiveRestService.class);

  private final LiveRestRepository repository;
  private final Wasserstand wasserstand;

  public LiveRestService(LiveRestRepository repository, Wasserstand wasserstand) {
    this.repository = repository;
    this.wasserstand = wasserstand;
  }

  /**
   * @param mandant Regel M2 — die Lesungen tragen die Mandantenkette im Statement
   * @param jetzt der Referenzzeitpunkt aus der Anwendungsuhr, Wanduhrzeit des Quellservers
   */
  public LiveRestErgebnis ermittle(MandantContext mandant, LocalDateTime jetzt) {
    LiveRestEntscheidung entscheidung = LiveRest.entscheide(wasserstand.wasserstand(), jetzt);
    if (!entscheidung.angewandt()) {
      return LiveRestErgebnis.ohneKorrektur(entscheidung);
    }
    LocalDateTime von = entscheidung.liveVon();
    LocalDateTime bis = entscheidung.liveBis();
    try {
      List<LiveRestZeile> ausDemRollup = repository.ausDemRollup(mandant, von, bis);
      List<LiveRestZeile> ausDerQuelle = repository.ausDerQuelle(mandant, von, bis);
      return new LiveRestErgebnis(entscheidung, LiveRestKorrektur.aus(ausDemRollup, ausDerQuelle));
    } catch (DataAccessException fehler) {
      LOG.warn(
          "Live-Rest ausgefallen, die Zahlen bleiben beim Rollup (vollstaendig bis {}).",
          von,
          fehler);
      return LiveRestErgebnis.ohneKorrektur(LiveRestEntscheidung.ausgesetztAb(von));
    }
  }
}
