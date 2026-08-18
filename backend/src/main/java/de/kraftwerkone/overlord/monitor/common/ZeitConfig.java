package de.kraftwerkone.overlord.monitor.common;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
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
   * Die Untergrenze des Fensters, in dem der Dev-Anker liegen darf.
   *
   * <p><b>Der 24.07.2025 ist der Tag nach der Rotationsgrenze.</b> Die Ablagen haengen am Zeitraum
   * und nicht am Mandanten; die Grenze liegt taggenau auf dem 23.07.2025 (M53 Teil B). Alles davor
   * verweist auf {@code FILESTOREPROD07}/{@code 08}, und die sind abgeschaltet — <b>63,2 % des
   * Bestands</b>, 2.111.355 von 3.341.519 Nachrichten, sind von hier aus nicht abrufbar.
   */
  private static final LocalDate ANKER_FRUEHESTENS = LocalDate.of(2025, 7, 24);

  /**
   * Die Obergrenze desselben Fensters.
   *
   * <p><b>Am 30.12.2025 endet der dichte Bestand.</b> Danach folgen fuenf leere Monate und einige
   * verstreute Tage; und am juengeren Ende laufen Datenbankkopie und Filestore-Kopie vollstaendig
   * auseinander — <b>153 von 153</b> Verweisen des 08.07.2026 liefern nichts (M66 (2)).
   */
  private static final LocalDate ANKER_SPAETESTENS = LocalDate.of(2025, 12, 30);

  /**
   * Der Anker der Dev-Uhr: der <b>juengste Zeitpunkt an einem Tag, an dem mindestens {@value
   * #MINDESTENS_MANDANTEN} Mandanten Nachrichten haben</b> — und der im abrufbaren Fenster liegt.
   *
   * <p>Der Wert wird <b>ermittelt und nicht eingetragen</b> — er ueberlebt damit eine Neubefuellung
   * der Testkopie. Gelesen wird ausschliesslich ein Zeitpunkt; die Abfrage liefert keine fachlichen
   * Daten und keine Mandanten-Identitaet, nur einen Tag, eine Anzahl und einen Zeitstempel.
   *
   * <p><b>Das Fenster ist am 18.08.2026 hinzugekommen (Schritt 8)</b>, weil der Rohdatenzugriff
   * ausserhalb davon nichts findet: {@code 2025-07-24} bis {@code 2025-12-30} ist das einzige
   * Fenster, in dem sich Datenbankkopie und Filestore-Kopie decken ({@code docs/rohdaten.md} §12
   * und §13, Punkt 5). Davor sind die Ablagen abgeschaltet, danach hat die Filestore-Kopie die
   * Dateien nicht.
   *
   * <p><b>Es ist ein Riegel und keine Korrektur.</b> Gemessen am 18.08.2026 liefert die Abfrage
   * <i>mit</i> und <i>ohne</i> Fenster denselben Anker, {@code 2025-12-30 04:09:47}. Der Anker lag
   * also schon vorher richtig, und zwar seit dem 01.08.2026: Damals ist {@code
   * MAX(Message.MessageLastUpdate)} durch die Mandantenbedingung ersetzt worden, und die schliesst
   * die vereinzelten Tage in {@code 2026-06} und {@code 2026-07} von selbst aus — dort haben an
   * keinem Tag drei Mandanten Daten. <b>Ohne das Fenster haengt diese Eigenschaft aber am Bestand
   * und nicht an einer Regel.</b> Eine Neubefuellung der Testkopie mit drei Mandanten an einem
   * jungen Tag genuegte, und der Rohdatenzugriff saehe lokal ab dem ersten Start kaputt aus — auf
   * eine Weise, die nach einem Fehler im eigenen Code aussieht. Genau das schliesst das Fenster
   * aus.
   *
   * <p><b>Im Profil {@code prod} aendert sich dadurch nichts</b>: Dort ist die Anwendungsuhr die
   * Systemuhr, diese Abfrage laeuft gar nicht.
   */
  private static final String ANKER_ABFRAGE =
      """
      select max(m.MessageLastUpdate)
      from Message m
      join Process p on p.ProcessID = m.ProcessID
      join ProjectMandant pm on pm.ProjectID = p.ProjectID
      where m.MessageLastUpdate >= ?
        and m.MessageLastUpdate < ?
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
   * <p><b>Beide Stufen liegen im abrufbaren Fenster {@code 2025-07-24} bis {@code 2025-12-30}</b>
   * (ergaenzt am 18.08.2026, Schritt 8). Ausserhalb davon liefert der Filestore nichts, und der
   * Rohdatenzugriff saehe lokal ab dem ersten Start kaputt aus. Die Begruendung steht bei {@link
   * #ANKER_ABFRAGE}; im Profil {@code prod} aendert sich nichts.
   *
   * <p><b>Drei Stufen, jede protokolliert.</b> Findet die Anker-Abfrage keinen Tag, wird auf das
   * Maximum <i>innerhalb desselben Fensters</i> zurueckgefallen; ist auch das leer oder schlaegt
   * etwas fehl, auf die Systemuhr. Ein stiller Rueckfall waere schlimmer als ein falscher Anker,
   * weil beide dieselbe leere Liste erzeugen und nur einer davon erklaerbar ist.
   */
  @Bean
  @Primary
  @Profile("dev")
  public Clock devClock(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    Clock system = Clock.systemDefaultZone();
    try {
      String herkunft =
          "juengster Tag mit mindestens "
              + MINDESTENS_MANDANTEN
              + " Mandanten im abrufbaren Fenster "
              + ANKER_FRUEHESTENS
              + " bis "
              + ANKER_SPAETESTENS;
      LocalDateTime anker = anker(glassfishDsl);
      if (anker == null) {
        log.warn(
            "Dev-Clock: Kein Tag mit mindestens {} Mandanten im Fenster {} bis {} gefunden —"
                + " Rueckfall auf das Maximum innerhalb desselben Fensters. Das"
                + " Standard-Zeitfenster kann dort sehr wenige Zeilen liefern.",
            MINDESTENS_MANDANTEN,
            ANKER_FRUEHESTENS,
            ANKER_SPAETESTENS);
        anker = maximum(glassfishDsl);
        herkunft = "MAX(MessageLastUpdate) im abrufbaren Fenster, Rueckfall";
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

  /** Der Anker aus Messung M9, seit 18.08.2026 zusaetzlich auf das abrufbare Fenster begrenzt. */
  private static LocalDateTime anker(DSLContext glassfishDsl) {
    // Unqualifiziert: die Verbindung des Lese-Pools zeigt bereits auf das GlassfishDB-Schema.
    return glassfishDsl
        .resultQuery(ANKER_ABFRAGE, fensterVon(), fensterBis(), MINDESTENS_MANDANTEN)
        .fetchOne(0, LocalDateTime.class);
  }

  /**
   * Der bisherige Anker, ab dem 01.08.2026 nur noch Rueckfallebene — <b>und seit dem 18.08.2026
   * ebenfalls auf das abrufbare Fenster begrenzt</b>.
   *
   * <p>Ohne diese Begrenzung waere der Rueckfall die Hintertuer, durch die genau das zurueckkaeme,
   * was das Fenster verhindern soll: {@code MAX(Message.MessageLastUpdate)} zeigt auf den
   * 08.07.2026, und dort liefert der Filestore nichts (M66 (2)). Ein Rueckfall, der die Regel der
   * ersten Stufe aufhebt, ist keiner.
   */
  private static LocalDateTime maximum(DSLContext glassfishDsl) {
    return glassfishDsl
        .resultQuery(
            "select max(MessageLastUpdate) from Message"
                + " where MessageLastUpdate >= ? and MessageLastUpdate < ?",
            fensterVon(),
            fensterBis())
        .fetchOne(0, LocalDateTime.class);
  }

  /** Untergrenze des abrufbaren Fensters, als Zeitpunkt. */
  private static LocalDateTime fensterVon() {
    return ANKER_FRUEHESTENS.atStartOfDay();
  }

  /**
   * Obergrenze des abrufbaren Fensters, als <b>ausschliessender</b> Zeitpunkt: der Beginn des
   * Folgetags. So gehoert der 30.12.2025 mit allen seinen Zeitstempeln noch dazu — mit {@code <=
   * 2025-12-30 00:00:00} waere er es nicht.
   */
  private static LocalDateTime fensterBis() {
    return ANKER_SPAETESTENS.plusDays(1).atStartOfDay();
  }

  /** Sicherheits- und Protokollzeit: immer die echte Uhr, in UTC. Kein Dev-Versatz. */
  @Bean("systemClock")
  public Clock systemClock() {
    return Clock.systemUTC();
  }
}
