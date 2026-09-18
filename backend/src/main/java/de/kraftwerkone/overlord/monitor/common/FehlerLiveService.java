package de.kraftwerkone.overlord.monitor.common;

import java.time.LocalDateTime;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <b>Der Baustein Fehler live</b>: die Fehler des Fensters live lesen — oder, faellt die Lesung
 * aus, sagen, dass sie ausgesetzt ist ({@code docs/fehler-live.md} §4).
 *
 * <h2>Ein Uhrenschlag je Anfrage</h2>
 *
 * <p>{@code von} und {@code bis} kommen vom Verbraucher: das Fenster, das er aus seinem einen
 * {@code jetzt} gebildet hat — dasselbe, mit dem er Rollup und Live-Rest liest. Ein eigener
 * Uhrenschlag hier waere ein zweiter Stichtag.
 *
 * <h2>Faellt die Lesung aus, ist sie ausgesetzt — wie der Live-Rest (E-185 sinngemaess)</h2>
 *
 * <p><b>Dieselbe Ausnahmebehandlung wie in {@link LiveRestService}, nicht weiter und nicht
 * enger:</b> Jede {@link DataAccessException} — etwa die Zeitgrenze des Lese-Pools — ergibt {@link
 * FehlerLiveZustand#AUSGESETZT} und ein {@code WARN} im Protokoll; der Verbraucher rechnet dann wie
 * vor diesem Schritt, und die Oberflaeche sagt, dass nachverarbeitete Nachrichten noch als Fehler
 * zaehlen koennen. Was keine {@code DataAccessException} ist, laeuft durch — ein Fehler in der
 * Zeilenabbildung ist kein ausgefallener Rest, sondern ein Fehler.
 */
@Service
public class FehlerLiveService {

  private static final Logger LOG = LoggerFactory.getLogger(FehlerLiveService.class);

  private final FehlerLiveRepository repository;

  public FehlerLiveService(FehlerLiveRepository repository) {
    this.repository = repository;
  }

  /**
   * @param mandant Regel M2 — die Lesung traegt die Mandantenkette im Statement
   * @param von der Anfang des Fensters, einschliessend, Wanduhrzeit des Quellservers
   * @param bis das Ende des Fensters, ausschliessend
   */
  public FehlerLiveErgebnis ermittle(MandantContext mandant, LocalDateTime von, LocalDateTime bis) {
    try {
      return FehlerLiveErgebnis.angewandt(repository.ausDerQuelle(mandant, von, bis));
    } catch (DataAccessException fehler) {
      LOG.warn(
          "Fehler live ausgefallen, die Fehler bleiben beim Rollup (Fenster {} bis {}).",
          von,
          bis,
          fehler);
      return FehlerLiveErgebnis.ausgesetzt();
    }
  }
}
