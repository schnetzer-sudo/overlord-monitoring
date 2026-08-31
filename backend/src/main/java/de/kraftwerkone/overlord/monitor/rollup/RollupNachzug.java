package de.kraftwerkone.overlord.monitor.rollup;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <b>Der einmalige Rueckwaertslauf der abgeleiteten Ebenen</b> — Tagesebene (Schritt 10b-1, Teil
 * C.3) und seit Schritt 10b-2 auch Monatsebene.
 *
 * <p>Nach einer Migration steht die neue Ebene leer da, waehrend die naechstfeinere den
 * Gesamtbestand traegt. Der laufende Job zieht sie nur fuer den Zeitraum nach, den sein Fenster
 * beruehrt — die uebrigen 646 Tage beziehungsweise 22 Monate der Testkopie bekaeme er nie zu
 * fassen. Dieser Lauf holt sie einmal nach.
 *
 * <p><b>Er hiess bis zum 31.08.2026 {@code RollupTagNachzug}</b>, und der Schalter hiess {@code
 * tagesebene-nachziehen}. Beides ist mit der dritten Ebene falsch geworden: Der Lauf zieht jetzt
 * <b>beide</b> abgeleiteten Ebenen nach.
 *
 * <h2>Beide Ebenen, in einem Schalter — und das ist der Punkt</h2>
 *
 * <p><b>Zwei getrennte Schalter waeren die Einladung, die Tagesebene neu zu rechnen und die
 * Monatsebene stehen zu lassen.</b> Danach staenden zwei Ebenen auf zwei Staenden, und das
 * Dashboard zeigte je nach Fensterbreite verschiedene Zahlen, ohne dass irgendwo ein Fehler
 * protokolliert waere — genau der Zustand, gegen den {@code ersetzeFenster} seine eine Transaktion
 * haelt. Was zusammengehoert, wird zusammen ausgeloest.
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
public class RollupNachzug {

  private static final Logger log = LoggerFactory.getLogger(RollupNachzug.class);

  private final RollupSchreibRepository schreibRepository;
  private final RollupUhren uhren;
  private final RollupEigenschaften eigenschaften;

  RollupNachzug(
      RollupSchreibRepository schreibRepository,
      RollupUhren uhren,
      RollupEigenschaften eigenschaften) {
    this.schreibRepository = schreibRepository;
    this.uhren = uhren;
    this.eigenschaften = eigenschaften;
  }

  /**
   * Rechnet <b>beide abgeleiteten Ebenen</b> fuer jeden Zeitraum neu, den die Stundenebene traegt.
   *
   * <p><b>Je Scheibe erst der Tag, dann der Monat</b>, und nicht erst alle Tage und danach alle
   * Monate. Zwei Gruende, und der zweite ist der wichtigere:
   *
   * <ol>
   *   <li>Ein Monatseimer ist die Summe seiner Tageseimer. Eine Scheibe ist an den Monatsgrenzen
   *       geschnitten und beruehrt deshalb <b>genau einen</b> Kalendermonat; wenn sie an der Reihe
   *       ist, stehen alle Tage dieses Monats bereits geschrieben, die es ueberhaupt gibt.
   *   <li><b>Nach jeder Scheibe sind beide Ebenen fuer diesen Monat auf demselben Stand.</b>
   *       Braeche der Lauf ab, waere das Ergebnis eine <i>kuerzere</i> Historie und keine
   *       widerspruechliche.
   * </ol>
   *
   * @return was er getan hat; leer, wenn die Stundenebene leer ist — dann gibt es nichts abzuleiten
   */
  public Ergebnis fuehreAus() {
    LocalDateTime begonnen = uhren.protokollzeit();
    Optional<RollupFenster> bereich = schreibRepository.bereichDerStundenebene();
    if (bereich.isEmpty()) {
      log.warn(
          "Rueckwaertslauf der abgeleiteten Ebenen: message_rollup ist leer, es gibt nichts"
              + " abzuleiten.");
      return new Ergebnis(0, 0, 0, 0, Duration.ZERO);
    }

    List<RollupFenster> scheiben = bereich.orElseThrow().monatsscheiben();
    log.warn(
        "Rueckwaertslauf der abgeleiteten Ebenen ueber {} in {} Scheibe(n). Das ist kein"
            + " Normalbetrieb — im Normalbetrieb zieht jeder Lauf seine eigenen Tage und Monate"
            + " nach.",
        bereich.orElseThrow(),
        scheiben.size());

    int tageszeilen = 0;
    int monatszeilen = 0;
    long tage = 0;
    for (int i = 0; i < scheiben.size(); i++) {
      if (i > 0) {
        drossle();
      }
      RollupFenster scheibe = scheiben.get(i);
      // Eine Scheibe ist nie leer.
      RollupFenster.Tagesbereich tagesbereich = scheibe.betroffeneTage().orElseThrow();
      RollupFenster.Monatsbereich monatsbereich = scheibe.betroffeneMonate().orElseThrow();

      int tagesZeilenDerScheibe = schreibRepository.rechneTageEbeneNeu(tagesbereich);
      int monatsZeilenDerScheibe = schreibRepository.rechneMonatsEbeneNeu(monatsbereich);

      tageszeilen += tagesZeilenDerScheibe;
      monatszeilen += monatsZeilenDerScheibe;
      tage += tagesbereich.tage();
      log.debug(
          "Scheibe {} bis {}: {} Tageszeilen, {} Monatszeilen",
          tagesbereich.erster(),
          tagesbereich.letzter(),
          tagesZeilenDerScheibe,
          monatsZeilenDerScheibe);
    }

    Duration dauer = Duration.between(begonnen, uhren.protokollzeit());
    log.warn(
        "Rueckwaertslauf der abgeleiteten Ebenen fertig: {} Tageszeilen und {} Monatszeilen ueber"
            + " {} Tage in {} Scheibe(n), {} ms.",
        tageszeilen,
        monatszeilen,
        tage,
        scheiben.size(),
        dauer.toMillis());
    return new Ergebnis(scheiben.size(), tageszeilen, monatszeilen, tage, dauer);
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
      throw new IllegalStateException(
          "Rueckwaertslauf der abgeleiteten Ebenen unterbrochen", unterbrochen);
    }
  }

  /**
   * Was der Rueckwaertslauf getan hat.
   *
   * @param scheiben in wie viele Monatsscheiben der Bereich zerlegt worden ist
   * @param tageszeilen Zeilen in {@code message_rollup_tag}. Ueber den Gesamtbestand der Testkopie
   *     sind das <b>123.049</b> (gerechnet am 27.08.2026 gegen 335.610 Stundenzeilen)
   * @param monatszeilen Zeilen in {@code message_rollup_monat}. M87, Variante 4 sagt <b>11.957</b>
   *     ueber den Gesamtbestand voraus — und die Zahl ist dort nicht geschaetzt, sondern an der
   *     Quelle gezaehlt
   * @param tage wie viele Kalendertage beruehrt worden sind — <b>alle</b>, auch die leeren: Der
   *     Lauf iteriert ueber einen Kalender und nicht ueber die vorhandenen Daten
   * @param dauer Laufzeit, gemessen mit {@code systemClock} wie jede Protokollzeit
   */
  public record Ergebnis(
      int scheiben, int tageszeilen, int monatszeilen, long tage, Duration dauer) {}
}
