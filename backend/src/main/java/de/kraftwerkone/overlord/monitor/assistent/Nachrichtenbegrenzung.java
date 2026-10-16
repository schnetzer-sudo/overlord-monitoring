package de.kraftwerkone.overlord.monitor.assistent;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Hoechstens 60 Nachrichten je Nutzer und Stunde (E‑1036), gleitend ueber die letzte Stunde —
 * <b>nur im Arbeitsspeicher</b>, wie die Begrenzung je IP ({@code security.AnmeldeSperre}): Ein
 * Zaehler in der Datenbank machte den Schutz selbst zur Last. Der Preis: Nach einem Neustart
 * beginnt sie von vorn.
 *
 * <p>Gezaehlt wird jede angenommene Nachricht, auch eine, an der der Anbieter scheitert (F8): Die
 * Grenze schuetzt vor Last und Kosten. Gerechnet wird mit der Systemuhr (F13).
 */
@Component
public class Nachrichtenbegrenzung {

  static final Duration FENSTER = Duration.ofHours(1);

  static final String PROBLEMTYP = "zu-viele-nachrichten";

  private final ConcurrentMap<Long, Deque<Instant>> zeiten = new ConcurrentHashMap<>();
  private final Clock uhr;
  private final int hoechstens;

  @Autowired
  Nachrichtenbegrenzung(@Qualifier("systemClock") Clock uhr, AssistentEigenschaften eigenschaften) {
    this(uhr, eigenschaften.nachrichtenJeStunde());
  }

  Nachrichtenbegrenzung(Clock uhr, int hoechstens) {
    this.uhr = uhr;
    this.hoechstens = hoechstens;
  }

  /**
   * Zaehlt die Nachricht, wenn der Nutzer sein Kontingent noch nicht ausgeschoepft hat.
   *
   * @throws FachlicheAusnahme {@code 429 zu-viele-nachrichten} sonst; dann wird nicht gezaehlt
   */
  public void pruefeUndZaehle(long nutzerId) {
    Instant jetzt = uhr.instant();
    Instant grenze = jetzt.minus(FENSTER);
    Deque<Instant> liste = zeiten.computeIfAbsent(nutzerId, unbenutzt -> new ArrayDeque<>());
    synchronized (liste) {
      while (!liste.isEmpty() && !liste.peekFirst().isAfter(grenze)) {
        liste.pollFirst();
      }
      if (liste.size() >= hoechstens) {
        throw new FachlicheAusnahme(
            HttpStatus.TOO_MANY_REQUESTS,
            PROBLEMTYP,
            "Zu viele Nachrichten",
            "Sie haben in der letzten Stunde "
                + hoechstens
                + " Nachrichten an den Assistenten geschickt. Bitte versuchen Sie es später"
                + " noch einmal.",
            "Begrenzung je Nutzer greift: " + liste.size() + " Nachrichten in der letzten Stunde",
            Map.of("hoechstensJeStunde", hoechstens));
      }
      liste.addLast(jetzt);
    }
  }
}
