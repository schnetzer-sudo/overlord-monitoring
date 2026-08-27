package de.kraftwerkone.overlord.monitor.rollup;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <b>Der einmalige Rueckwaertslauf der Tagesebene</b> (Schritt 10b-1, Teil C.3).
 *
 * <p>Nach der Migration steht {@code message_rollup_tag} leer da, waehrend {@code message_rollup}
 * den Gesamtbestand traegt. Der laufende Job zieht die Tagesebene nur fuer die Tage nach, die sein
 * Fenster beruehrt — die uebrigen 460 Tage der Testkopie bekaeme er nie zu fassen. Dieser Lauf holt
 * sie einmal nach.
 *
 * <h2>Warum das kein Volllauf ist</h2>
 *
 * <p>Ein Volllauf koennte dasselbe: Er rechnet beide Ebenen ueber den ganzen Bestand. Er ist hier
 * trotzdem der falsche Weg, aus zwei Gruenden.
 *
 * <ol>
 *   <li><b>Er liest {@code Message} noch einmal</b> — 45,772 s und rund 3,34 Millionen Zeilen ueber
 *       den Lese-Pool, den sich die Anwendung mit der Produktion teilt. Die Tagesebene steht
 *       vollstaendig in der Stundenebene; die Quelle dafuer noch einmal zu befragen, ist Arbeit
 *       ohne Erkenntnisgewinn.
 *   <li><b>Im Profil {@code dev} deckte er den Bestand gar nicht ab.</b> Seine obere Grenze ist die
 *       Anwendungsuhr, und die steht am Anker der Testkopie; die 15 Rollupzeilen aus Juni und Juli
 *       2026 laegen dahinter ({@code docs/rollup.md} §4). Dieser Lauf richtet sich nach dem, was in
 *       der Stundenebene <b>steht</b>, und nicht nach einer Uhr.
 * </ol>
 *
 * <h2>In Scheiben, ueber einen Kalender</h2>
 *
 * <p>Dieselbe Form wie beim Volllauf ({@code docs/rollup.md} §6): Monatsscheiben, ueber einen
 * Kalender und nicht ueber die vorhandenen Daten, mit der Drosselung aus Leistungsregel L6 zwischen
 * zwei Scheiben. <b>Anders als beim Volllauf ist die Scheibe hier keine gemessene Notwendigkeit,
 * sondern Vorsorge:</b> Das Statement lauft ueber den Schreib-Pool (30 s Zeitgrenze) und ueber eine
 * Tabelle von 21,6 MiB, nicht ueber die 2,7 GiB von {@code Message}. Eine Scheibe ist trotzdem
 * richtig — sie haelt die Transaktion klein und die Instanz frei.
 *
 * <p><b>Jede Scheibe ist ihre eigene Transaktion</b>, und das unterscheidet diesen Lauf vom
 * naechtlichen Volllauf. Dort ist die eine grosse Transaktion Absicht: Eine halb geleerte
 * Rolluptabelle um 03:00 waere ein leeres Dashboard. Hier ist der Ausgangszustand eine <b>leere</b>
 * Tagesebene; ein Abbruch nach der Haelfte hinterlaesst eine halb gefuellte, und die ist besser als
 * eine leere. Wiederholen laesst er sich ohnehin: Jede Scheibe loescht ihren Bereich, bevor sie ihn
 * schreibt.
 *
 * <h2>Er hinterlaesst keine Zeile in {@code rollup_lauf}</h2>
 *
 * <p><b>Und das ist Absicht, keine Nachlaessigkeit.</b> {@code rollup_lauf} traegt den
 * <b>Wasserstand</b>: bis wohin ist aus {@code Message} gerechnet worden. Dieser Lauf rechnet
 * nichts aus {@code Message}; er ordnet um, was schon da ist. Eine Zeile mit {@code art = 'VOLL'}
 * und einem Fenster ueber den Gesamtbestand behauptete einen Wasserstand, den er nicht erarbeitet
 * hat — und der naechste Delta-Lauf uebersaehe daraufhin einen Bereich. Seine Spur steht im
 * Protokoll.
 */
@Service
public class RollupTagNachzug {

  private static final Logger log = LoggerFactory.getLogger(RollupTagNachzug.class);

  private final RollupSchreibRepository schreibRepository;
  private final RollupUhren uhren;
  private final RollupEigenschaften eigenschaften;

  RollupTagNachzug(
      RollupSchreibRepository schreibRepository,
      RollupUhren uhren,
      RollupEigenschaften eigenschaften) {
    this.schreibRepository = schreibRepository;
    this.uhren = uhren;
    this.eigenschaften = eigenschaften;
  }

  /**
   * Rechnet die Tagesebene fuer <b>jeden</b> Tag neu, den die Stundenebene traegt.
   *
   * @return was er getan hat; leer, wenn die Stundenebene leer ist — dann gibt es nichts abzuleiten
   */
  public Ergebnis fuehreAus() {
    LocalDateTime begonnen = uhren.protokollzeit();
    Optional<RollupFenster> bereich = schreibRepository.bereichDerStundenebene();
    if (bereich.isEmpty()) {
      log.warn(
          "Rueckwaertslauf der Tagesebene: message_rollup ist leer, es gibt nichts abzuleiten.");
      return new Ergebnis(0, 0, 0, Duration.ZERO);
    }

    List<RollupFenster> scheiben = bereich.orElseThrow().monatsscheiben();
    log.warn(
        "Rueckwaertslauf der Tagesebene ueber {} in {} Scheibe(n). Das ist kein Normalbetrieb —"
            + " im Normalbetrieb zieht jeder Lauf seine eigenen Tage nach.",
        bereich.orElseThrow(),
        scheiben.size());

    int zeilen = 0;
    long tage = 0;
    for (int i = 0; i < scheiben.size(); i++) {
      if (i > 0) {
        drossle();
      }
      RollupFenster.Tagesbereich scheibe =
          scheiben.get(i).betroffeneTage().orElseThrow(); // Eine Scheibe ist nie leer.
      int geschrieben = schreibRepository.rechneTageEbeneNeu(scheibe);
      zeilen += geschrieben;
      tage += scheibe.tage();
      log.debug(
          "Scheibe {} bis {}: {} Tageszeilen", scheibe.erster(), scheibe.letzter(), geschrieben);
    }

    Duration dauer = Duration.between(begonnen, uhren.protokollzeit());
    log.warn(
        "Rueckwaertslauf der Tagesebene fertig: {} Zeilen ueber {} Tage in {} Scheibe(n),"
            + " {} ms.",
        zeilen,
        tage,
        scheiben.size(),
        dauer.toMillis());
    return new Ergebnis(scheiben.size(), zeilen, tage, dauer);
  }

  /**
   * Leistungsregel L6, hier mit demselben Wert wie beim Volllauf. Gewartet wird <b>zwischen</b>
   * zwei Scheiben und nicht vor der ersten oder nach der letzten.
   */
  private void drossle() {
    Duration pause = eigenschaften.scheibenPause();
    if (pause.isZero()) {
      return;
    }
    try {
      Thread.sleep(pause);
    } catch (InterruptedException unterbrochen) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Rueckwaertslauf der Tagesebene unterbrochen", unterbrochen);
    }
  }

  /**
   * Was der Rueckwaertslauf getan hat.
   *
   * @param scheiben in wie viele Monatsscheiben der Bereich zerlegt worden ist
   * @param zeilenGeschrieben Zeilen in {@code message_rollup_tag}. Ueber den Gesamtbestand der
   *     Testkopie sind das <b>123.049</b> (gerechnet am 27.08.2026 gegen 335.610 Stundenzeilen)
   * @param tage wie viele Kalendertage beruehrt worden sind — <b>alle</b>, auch die leeren: Der
   *     Lauf iteriert ueber einen Kalender und nicht ueber die vorhandenen Daten
   * @param dauer Laufzeit, gemessen mit {@code systemClock} wie jede Protokollzeit
   */
  public record Ergebnis(int scheiben, int zeilenGeschrieben, long tage, Duration dauer) {}
}
