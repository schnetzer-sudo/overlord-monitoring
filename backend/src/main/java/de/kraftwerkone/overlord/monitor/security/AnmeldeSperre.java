package de.kraftwerkone.overlord.monitor.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.kraftwerkone.overlord.monitor.common.error.ZuVieleAnmeldeversucheException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Begrenzung der Anmeldeversuche <b>je IP-Adresse</b> — ausschliesslich im Arbeitsspeicher.
 *
 * <p><b>Sie schreibt niemals in die Datenbank.</b> Genau das ist der Punkt: Ein Zaehler in einer
 * Tabelle machte den Schutzmechanismus selbst zum Angriffsvektor, weil jeder Bot mit jedem Versuch
 * Schreiblast auf einer Instanz erzeugte, die wir uns mit der EDI-Produktion teilen. Der Preis ist,
 * dass die Begrenzung bei einem Neustart und je Instanz von vorn beginnt — die <b>persistente</b>
 * Sperre liegt an {@code app_user} und ist der eigentliche Schutz des einzelnen Kontos.
 *
 * <p>Die Grenze ist bewusst grosszuegiger als die Kontosperre: Hinter einer IP kann ein ganzes
 * Buero stehen (NAT), und ein zu enger Wert sperrte Kollegen aus, die nichts falsch gemacht haben.
 *
 * <p>{@code maximumSize} ist keine Feinheit: Ohne Obergrenze waere die Tabelle selbst der
 * Angriffspunkt — Anfragen von vielen gefaelschten Adressen liessen den Speicher wachsen.
 */
@Component
public class AnmeldeSperre {

  /** Fehlversuche je IP im Zeitfenster, danach wird abgewiesen. */
  static final int GRENZE_JE_IP = 20;

  /** Fenster, gerechnet ab dem ersten Fehlversuch der IP. */
  static final Duration FENSTER = Duration.ofMinutes(15);

  private static final int MAX_ADRESSEN = 10_000;

  private final Cache<String, AtomicInteger> fehlversuche =
      Caffeine.newBuilder().expireAfterWrite(FENSTER).maximumSize(MAX_ADRESSEN).build();

  /**
   * Weist ab, wenn diese IP ihr Kontingent ausgeschoepft hat.
   *
   * @throws ZuVieleAnmeldeversucheException sobald die Grenze erreicht ist
   */
  public void pruefe(String ip) {
    AtomicInteger zaehler = fehlversuche.getIfPresent(ip);
    if (zaehler != null && zaehler.get() >= GRENZE_JE_IP) {
      throw new ZuVieleAnmeldeversucheException(
          "IP-Begrenzung greift: " + zaehler.get() + " Fehlversuche im laufenden Fenster");
    }
  }

  /** Ein fehlgeschlagener Versuch von dieser IP. */
  public void zaehleFehlversuch(String ip) {
    fehlversuche.get(ip, unbenutzt -> new AtomicInteger()).incrementAndGet();
  }

  /** Erfolgreiche Anmeldung: die IP ist offensichtlich in Ordnung. */
  public void zuruecksetzen(String ip) {
    fehlversuche.invalidate(ip);
  }
}
