package de.kraftwerkone.overlord.monitor.common;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Die Zeitquellen des Projekts. Bewusst in {@code common}: die einzige Stelle, die die Systemuhr
 * liest, ist diese Konfiguration ({@code Clock.systemDefaultZone()} bzw. {@code Clock.systemUTC()}
 * sind erlaubt; {@code LocalDateTime.now()} und Verwandte werden nirgends aufgerufen, Regel Z1).
 *
 * <p>Es gibt zwei Uhren:
 *
 * <ul>
 *   <li><b>Anwendungsuhr</b> ({@link Primary}): relative Zeitfenster und Timeout-Berechnung. In
 *       Produktion die Systemuhr, im Dev-Profil um den Rueckstand der Testkopie zurueckversetzt.
 *   <li><b>{@code systemClock}</b>: sicherheitsrelevante Zeit (Sitzungsablauf, Sperrfristen ab
 *       Schritt 3) und Protokollzeit. Nutzt <b>niemals</b> den Dev-Versatz, immer die echte Uhr in
 *       UTC.
 * </ul>
 */
@Configuration
public class ZeitConfig {

  private static final Logger log = LoggerFactory.getLogger(ZeitConfig.class);

  /** Anwendungsuhr in Produktion und ausserhalb des Dev-Profils: die Systemuhr. */
  @Bean
  @Primary
  @Profile("!dev")
  public Clock clock() {
    return Clock.systemDefaultZone();
  }

  /**
   * Wie viele Mandanten ein Tag tragen muss, um als Anker zu taugen.
   *
   * <p>Drei, nicht einer: Ein Tag mit genau einem Mandanten ist genau der Zustand, den die
   * Umstellung beseitigen soll. Und nicht alle zehn — die Testkopie hat Mandanten mit neun
   * Nachrichten insgesamt ({@code NXHBE}) und solche ohne jede ({@code EDITIONLINGERI}); eine hohe
   * Schwelle faende nie einen Tag.
   */
  private static final int MINDESTENS_MANDANTEN = 3;

  /**
   * Der Anker der Dev-Uhr: der <b>juengste Zeitpunkt an einem Tag, an dem mindestens {@value
   * #MINDESTENS_MANDANTEN} Mandanten Nachrichten haben</b>.
   *
   * <p>Der Wert wird <b>ermittelt und nicht eingetragen</b> — er ueberlebt damit eine Neubefuellung
   * der Testkopie. Gelesen wird ausschliesslich ein Zeitpunkt; die Abfrage liefert keine fachlichen
   * Daten und keine Mandanten-Identitaet, nur einen Tag, eine Anzahl und einen Zeitstempel.
   */
  private static final String ANKER_ABFRAGE =
      """
      select max(m.MessageLastUpdate)
      from Message m
      join Process p on p.ProcessID = m.ProcessID
      join ProjectMandant pm on pm.ProjectID = p.ProjectID
      group by date(m.MessageLastUpdate)
      having count(distinct pm.MandantID) >= ?
      order by date(m.MessageLastUpdate) desc
      limit 1
      """;

  /**
   * Anwendungsuhr im Dev-Profil: Beim Start wird ein Anker aus der Testkopie gelesen und die Uhr um
   * den Rueckstand zurueckversetzt. Die Zeit laeuft weiter, sie friert nicht ein.
   *
   * <p><b>Warum nicht mehr {@code MAX(Message.MessageLastUpdate)} (geaendert 01.08.2026).</b>
   * Dieses Maximum zeigt auf den 08.07.2026 — und ein 24-Stunden-Fenster von dort enthaelt <b>285
   * Zeilen eines einzigen Mandanten</b>. Der Bestand der Testkopie ist dicht bis zum 30.12.2025,
   * danach folgen fuenf leere Monate und fuenf verstreute Tage, an denen nur {@code NEXANS} Daten
   * hat. Lokal sah damit jeder andere Mandant leer aus — genau das, was Regel Z1 verhindern soll.
   *
   * <p>Der neue Anker liefert im selben 24-Stunden-Fenster 6.382 Zeilen aus sechs Mandanten,
   * darunter beide Testmandanten des Isolationstests. Zahlen und Messung: {@code
   * docs/messungen-schritt4.md}, Abschnitt M9.
   *
   * <p><b>Drei Stufen, jede protokolliert.</b> Findet die Anker-Abfrage keinen Tag, wird auf {@code
   * MAX(Message.MessageLastUpdate)} zurueckgefallen; ist auch das leer oder schlaegt etwas fehl,
   * auf die Systemuhr. Ein stiller Rueckfall waere schlimmer als ein falscher Anker, weil beide
   * dieselbe leere Liste erzeugen und nur einer davon erklaerbar ist.
   */
  @Bean
  @Primary
  @Profile("dev")
  public Clock devClock(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    Clock system = Clock.systemDefaultZone();
    try {
      String herkunft = "juengster Tag mit mindestens " + MINDESTENS_MANDANTEN + " Mandanten";
      LocalDateTime anker = anker(glassfishDsl);
      if (anker == null) {
        log.warn(
            "Dev-Clock: Kein Tag mit mindestens {} Mandanten gefunden — Rueckfall auf"
                + " MAX(Message.MessageLastUpdate). Das Standard-Zeitfenster kann dort sehr wenige"
                + " Zeilen liefern.",
            MINDESTENS_MANDANTEN);
        anker = maximum(glassfishDsl);
        herkunft = "MAX(Message.MessageLastUpdate), Rueckfall";
      }
      if (anker == null) {
        log.warn(
            "Dev-Clock: Kein Anker in der Testkopie gefunden — es wird die Systemuhr verwendet.");
        return system;
      }
      Clock dev = DevClockFactory.ausAnker(anker, system);
      Duration versatz = Duration.between(system.instant(), dev.instant());
      log.warn(
          "Dev-Clock aktiv: Anwendungszeit auf {} zurueckversetzt (Versatz {} Tage / {} Stunden),"
              + " Anker: {}. Sicherheitsrelevante Zeit nutzt weiterhin die Systemuhr.",
          LocalDateTime.ofInstant(dev.instant(), system.getZone()),
          versatz.toDays(),
          versatz.toHours(),
          herkunft);
      return dev;
    } catch (RuntimeException ex) {
      log.warn(
          "Dev-Clock: Der Anker konnte nicht gelesen werden ({}) — es wird die Systemuhr verwendet.",
          ex.getMessage());
      return system;
    }
  }

  /** Der Anker aus Messung M9. {@code null}, wenn kein Tag die Bedingung erfuellt. */
  private static LocalDateTime anker(DSLContext glassfishDsl) {
    // Unqualifiziert: die Verbindung des Lese-Pools zeigt bereits auf das GlassfishDB-Schema.
    return glassfishDsl
        .resultQuery(ANKER_ABFRAGE, MINDESTENS_MANDANTEN)
        .fetchOne(0, LocalDateTime.class);
  }

  /** Der bisherige Anker, ab dem 01.08.2026 nur noch Rueckfallebene. */
  private static LocalDateTime maximum(DSLContext glassfishDsl) {
    return glassfishDsl
        .resultQuery("select max(MessageLastUpdate) from Message")
        .fetchOne(0, LocalDateTime.class);
  }

  /** Sicherheits- und Protokollzeit: immer die echte Uhr, in UTC. Kein Dev-Versatz. */
  @Bean("systemClock")
  public Clock systemClock() {
    return Clock.systemUTC();
  }
}
