package de.kraftwerkone.overlord.monitor.rollup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * <b>Der Weg, einen Lauf von Hand auszuloesen</b> — als Startparameter und nicht als HTTP-Endpunkt.
 *
 * <pre>{@code
 * ./mvnw spring-boot:run -Dspring-boot.run.arguments=--overlord.rollup.beim-start=VOLL
 * java -jar overlord-monitor.jar --overlord.rollup.beim-start=DELTA
 * }</pre>
 *
 * <p><b>Warum kein Endpunkt.</b> Ein Endpunkt, der die Rolluptabelle neu schreibt, waere eine
 * Schreibflaeche im Anfragepfad — und die gaebe es dann auch fuer den, der sie nicht bedienen soll.
 * Der Startparameter steht nur dem offen, der die Anwendung startet, und er hinterlaesst dieselbe
 * Spur in {@code rollup_lauf} wie jeder andere Lauf.
 *
 * <p><b>Er haengt nicht am Profil {@code rollup} und nicht an {@code overlord.rollup.aktiv}.</b>
 * Beide regeln die <i>zeitgesteuerte</i> Ausloesung; dieser Lauf ist ausdruecklich angefordert
 * worden. Genau deshalb ist er auch der Weg, auf dem der Volllauf zur Abnahme gefahren wird.
 *
 * <p><b>Die Ausnahme wird gefangen und nicht weitergereicht.</b> Ein gescheiterter Lauf soll den
 * Start der Anwendung nicht verhindern — die Oberflaeche haengt nicht am Rollup, und ein Backend,
 * das wegen eines Aggregationsfehlers gar nicht erst hochkommt, waere der groessere Schaden. Der
 * Grund steht in {@code rollup_lauf.fehler} und im Protokoll.
 */
@Component
public class RollupStarter implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(RollupStarter.class);

  private final RollupJob job;
  private final RollupNachzug nachzug;
  private final RollupEigenschaften eigenschaften;

  RollupStarter(RollupJob job, RollupNachzug nachzug, RollupEigenschaften eigenschaften) {
    this.job = job;
    this.nachzug = nachzug;
    this.eigenschaften = eigenschaften;
  }

  @Override
  public void run(ApplicationArguments args) {
    loeseLaufAus();
    loeseNachzugAus();
  }

  /**
   * Der Rueckwaertslauf der abgeleiteten Ebenen — <b>nach</b> dem Lauf und nicht davor. Beides
   * zusammen ist zulaessig; in dieser Reihenfolge zieht der Nachzug den Stand nach, den der Lauf
   * hinterlassen hat, und nicht den davor.
   */
  private void loeseNachzugAus() {
    if (!eigenschaften.abgeleiteteEbenenNachziehen()) {
      return;
    }
    try {
      nachzug.fuehreAus();
    } catch (RuntimeException fehler) {
      log.error(
          "Rueckwaertslauf der abgeleiteten Ebenen gescheitert. Die Anwendung startet trotzdem; was"
              + " schon geschrieben ist, bleibt richtig — jede Scheibe ist ihre eigene"
              + " Transaktion.",
          fehler);
    }
  }

  private void loeseLaufAus() {
    LaufArt art = eigenschaften.beimStart();
    if (art == null) {
      return;
    }
    log.warn(
        "Rollup-Lauf {} wird beim Start von Hand ausgeloest (overlord.rollup.beim-start). Das ist"
            + " kein Normalbetrieb — im Normalbetrieb loest der Zeitplan aus.",
        art);
    RollupFenster fenster = job.ermittleFenster(art);
    try {
      RollupErgebnis ergebnis = job.fuehreAus(fenster.von(), fenster.bis(), art);
      log.warn(
          "Rollup-Lauf {} von Hand beendet: {} Zeilen fuer {} Nachrichten aus {} Scheibe(n) in"
              + " {} ms. Fenster {}.",
          art,
          ergebnis.zeilenGeschrieben(),
          ergebnis.nachrichten(),
          ergebnis.scheiben(),
          ergebnis.dauer().toMillis(),
          ergebnis.fenster());
    } catch (RuntimeException fehler) {
      log.error(
          "Rollup-Lauf {} von Hand gescheitert. Die Anwendung startet trotzdem; der Grund steht in"
              + " rollup_lauf.fehler.",
          art,
          fehler);
    }
  }
}
