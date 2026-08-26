package de.kraftwerkone.overlord.monitor.rollup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Die zeitgesteuerte Ausloesung: stuendlich ein Delta-Lauf, naechtlich ein Volllauf.
 *
 * <h2>Zwei Riegel, und beide sind Absicht</h2>
 *
 * <ol>
 *   <li><b>Das Profil {@link RollupEigenschaften#PROFIL}.</b> Ohne es gibt es diese Bean nicht —
 *       und damit im Profil {@code dev} standardmaessig keine Ausloesung. Ein Job, der beim lokalen
 *       Start unaufgefordert in die geteilte Testkopie schreibt, ist eine Ueberraschung.
 *   <li><b>{@code overlord.rollup.aktiv}.</b> Damit laesst sich die Ausloesung abschalten, ohne das
 *       Profil zu aendern — im Betrieb kommt das Profil aus einer Umgebungsvariablen, und eine
 *       Abschaltung ueber sie kostete eine Neubereitstellung.
 * </ol>
 *
 * <p>Die beiden Ausdruecke stehen als Platzhalter und nicht als Literal: {@code application.yml}
 * ist die Stelle, an der ablesbar ist, wann der Job laeuft. Fehlt einer der Schluessel, scheitert
 * der Start — das ist besser als ein Zeitplan, den niemand kennt.
 *
 * <p><b>Ein HTTP-Endpunkt zum Ausloesen entsteht ausdruecklich nicht.</b> Der Weg von Hand fuehrt
 * ueber {@link RollupStarter} und einen Startparameter.
 */
@Component
@Profile(RollupEigenschaften.PROFIL)
@ConditionalOnProperty(prefix = "overlord.rollup", name = "aktiv", havingValue = "true")
public class RollupPlaner {

  private static final Logger log = LoggerFactory.getLogger(RollupPlaner.class);

  private final RollupJob job;
  private final RollupSchreibRepository schreibRepository;
  private final RollupUhren uhren;

  RollupPlaner(RollupJob job, RollupSchreibRepository schreibRepository, RollupUhren uhren) {
    this.job = job;
    this.schreibRepository = schreibRepository;
    this.uhren = uhren;
  }

  /**
   * Der stuendliche Delta-Lauf.
   *
   * <p><b>Nicht zur vollen Stunde, sondern kurz danach</b> — die Vorgabe ist {@code 0 5 * * * *}.
   * Die Minute ist <b>nicht gemessen und auch folgenlos</b>: Weil jeder Lauf ganze Stundeneimer
   * ersetzt und um {@value RollupFenster#NACHLAUF_MINUTEN} Minuten zurueckgreift, wird die zuletzt
   * geschriebene Stunde beim naechsten Lauf ohnehin neu gerechnet. Der Abstand zur vollen Stunde
   * haelt den Job nur aus dem Gedraenge der Jobs heraus, die dort ueblicherweise starten.
   */
  @Scheduled(cron = "${overlord.rollup.delta-plan}")
  public void stuendlich() {
    starte(LaufArt.DELTA);
  }

  /**
   * Der naechtliche Volllauf.
   *
   * <p><b>Die Uhrzeit ist ungemessen (Regel Q4).</b> Die Verteilung von {@code MessageLastUpdate}
   * ueber die Tagesstunde ist in M86 bis M92 nicht erhoben worden; {@code 03:00} ist die uebliche
   * Wahl und keine hergeleitete. Sie steht als offener Punkt in {@code docs/rollup.md}.
   *
   * <p>Er rechnet den Bestand von vorn und heilt damit, was ein Delta-Lauf verfehlt haben koennte:
   * eine Zeile, die spaeter als das Nachlauffenster nachgeschrieben wurde, oder ein Lauf, der
   * abgebrochen ist.
   */
  @Scheduled(cron = "${overlord.rollup.voll-plan}")
  public void naechtlich() {
    starte(LaufArt.VOLL);
  }

  /**
   * <b>Nur eine Ausfuehrung gleichzeitig.</b> Laeuft bereits einer, wird der neue uebersprungen und
   * das protokolliert — <b>auf WARN und nicht auf DEBUG</b>: Ein uebersprungener Lauf ist kein
   * Normalfall, sondern der Hinweis darauf, dass ein Lauf laenger braucht als sein Takt.
   *
   * <p>Zwei gleichzeitige Laeufe waeren fachlich nicht einmal falsch — sie loeschen und schreiben
   * dieselben Eimer —, aber sie verdoppelten die Last auf einer Instanz, die sich mit der
   * Produktion teilt (Leistungsregel L6).
   *
   * <p><b>Die Ausnahme wird hier gefangen und nicht weitergereicht.</b> Der Lauf hat sie bereits in
   * {@code rollup_lauf.fehler} vermerkt; wuerde sie aus einer {@code @Scheduled}-Methode
   * herausfliegen, staende sie ein zweites Mal im Protokoll und saehe nach zwei Fehlern aus.
   */
  private void starte(LaufArt art) {
    if (schreibRepository.laeuftBereits(uhren.protokollzeit())) {
      log.warn(
          "Rollup-Lauf {} uebersprungen: Es laeuft bereits einer. Braucht ein Lauf laenger als"
              + " sein Takt, ist das ein Befund und keine Kleinigkeit.",
          art);
      return;
    }
    RollupFenster fenster = job.ermittleFenster(art);
    try {
      job.fuehreAus(fenster.von(), fenster.bis(), art);
    } catch (RuntimeException fehler) {
      log.error(
          "Rollup-Lauf {} gescheitert. Der Zeitplan laeuft weiter; der Grund steht in"
              + " rollup_lauf.fehler.",
          art,
          fehler);
    }
  }
}
