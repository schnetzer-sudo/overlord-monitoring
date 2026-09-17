package de.kraftwerkone.overlord.monitor.common;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * <b>Der Live-Rest der laufenden Stunde — die Entscheidung</b>, als reine Funktion ohne Datenbank
 * ({@code docs/live-rest.md}).
 *
 * <h2>Die Lage, die es zu loesen gibt</h2>
 *
 * <p>Der Prozessbaum liest die Rollup-Ebene seines Fensters, und das Fenster schliesst den
 * angebrochenen Eimer ein. Der Delta-Lauf rechnet diesen Eimer nur einmal pro Stunde neu ({@code 0
 * 5 * * * *}); die Uebertragungsliste liest dasselbe Fenster live aus {@code Message} (E-50 in
 * {@code docs/process-view.md}). Kurz vor dem naechsten Lauf fehlt im Baum bis zu eine Stunde
 * Verkehr, und links und rechts stehen zwei Zahlen. Entschieden vom Auftraggeber am 16.09.2026:
 * <b>Live-Rest statt kuerzerem Takt</b>, ein Baustein fuer Prozessbaum und Dashboard.
 *
 * <h2>Die Begriffe</h2>
 *
 * <ul>
 *   <li><b>W</b>, der Wasserstand: {@code fenster_bis} des juengsten abgeschlossenen und
 *       fehlerfreien Laufs ({@link Wasserstand}). Datenzeit der Anwendungsuhr, ein voller
 *       Stundenanfang.
 *   <li><b>G</b> = W minus eine Stunde: der Stundeneimer, in dem dieser Lauf lief. Eimer vor G sind
 *       mit ihm vollstaendig gerechnet, der Eimer G nur bis zum Laufzeitpunkt.
 *   <li><b>Live-Bereich</b>: von G bis zum Anfang der Stunde nach {@code jetzt}, halboffen. {@code
 *       jetzt} kommt aus der Anwendungsuhr (Regel Z1) — der Aufrufer schlaegt sie genau einmal je
 *       Anfrage und reicht den Wert herein.
 * </ul>
 *
 * <h2>Die Stundenarithmetik ist dieselbe wie in {@code RollupFenster}</h2>
 *
 * <p>Der Anfang der Stunde ist {@code truncatedTo(HOURS)}, der Anfang der naechsten Stunde {@code
 * truncatedTo(HOURS).plusHours(1)} — auch dann, wenn {@code jetzt} genau auf einer vollen Stunde
 * liegt. Gerechnet wird auf {@link LocalDateTime}, also auf den <b>Stundenlabels der Wanduhr</b>,
 * genau wie {@code message_rollup.stunde} sie fuehrt: Am Umstellungstag gibt es ein Label doppelt
 * oder gar nicht, und die Eimer folgen dem Label, nicht der Sonne.
 */
public final class LiveRest {

  /**
   * <b>Ab wann der Live-Rest ausgesetzt wird: mehr als drei Stunden zwischen G und jetzt.</b>
   *
   * <p>Eine benannte Konstante mit Begruendung und kein Konfigurationsschluessel — dieselbe Haltung
   * wie {@code RollupFenster.NACHLAUF_MINUTEN}: Ein Drehknopf luede dazu ein, den Wert ohne neue
   * Ueberlegung zu verstellen. Normal liegen hoechstens rund 65 Minuten zwischen G und jetzt (der
   * Lauf um {@code hh:05} rechnet den Eimer {@code hh:00}, der Baum fragt bis {@code hh+1:04}); mit
   * einem uebersprungenen Lauf sind es gut zwei Stunden. Drei Stunden fangen einen ausgefallenen
   * Lauf und lassen zwei ausgefallene als das erscheinen, was sie sind: eine Stoerung, bei der die
   * Zahlen unvollstaendig sind und die Oberflaeche es sagt. Ein groesserer Live-Bereich hiesse
   * ausserdem, je Anfrage mehr Stunden live aus {@code Message} zu zaehlen — und genau das ist der
   * Zugriff, den Regel L2 auf das Noetige beschraenkt.
   */
  public static final int OBERGRENZE_STUNDEN = 3;

  /** {@link #OBERGRENZE_STUNDEN} als Dauer. */
  public static final Duration OBERGRENZE = Duration.ofHours(OBERGRENZE_STUNDEN);

  private LiveRest() {}

  /**
   * Die Entscheidung.
   *
   * <ol>
   *   <li>kein abgeschlossener fehlerfreier Lauf → {@code AUSGESETZT}, ohne Zeitangabe
   *   <li>G liegt <b>nach</b> dem Anfang der aktuellen Stunde → {@code NICHT_NOETIG}: Der Rollup
   *       reicht ueber die Uhr hinaus
   *   <li>zwischen G und jetzt liegen <b>mehr als</b> {@value #OBERGRENZE_STUNDEN} Stunden → {@code
   *       AUSGESETZT}, vollstaendig bis G
   *   <li>sonst → {@code ANGEWANDT}, Live-Bereich von G bis zum Anfang der Stunde nach jetzt
   * </ol>
   *
   * <p>Liegt G <b>genau</b> auf dem Anfang der aktuellen Stunde, wird gerechnet (der Eimer ist
   * angebrochen); liegen <b>genau</b> drei Stunden zwischen G und jetzt, wird noch gerechnet. Beide
   * Grenzen sind so gezogen, dass im Zweifel korrigiert wird — ein zu grosser Live-Bereich kostet
   * eine Stunde Zaehlung, ein zu kleiner kostet Zahlen.
   *
   * @param wasserstand W, oder leer, wenn nie gerechnet wurde. Per Konstruktion ein voller
   *     Stundenanfang ({@code RollupFenster}); waere er es nicht, rundet {@link #laufeimer} ab und
   *     deckt einen Eimer mehr ab — nie einen weniger
   * @param jetzt der Referenzzeitpunkt aus der Anwendungsuhr, Wanduhrzeit des Quellservers
   */
  public static LiveRestEntscheidung entscheide(
      Optional<LocalDateTime> wasserstand, LocalDateTime jetzt) {
    if (wasserstand.isEmpty()) {
      return LiveRestEntscheidung.ausgesetztOhneLauf();
    }
    LocalDateTime g = laufeimer(wasserstand.get());
    LocalDateTime aktuelleStunde = jetzt.truncatedTo(ChronoUnit.HOURS);
    if (g.isAfter(aktuelleStunde)) {
      return LiveRestEntscheidung.nichtNoetig();
    }
    if (Duration.between(g, jetzt).compareTo(OBERGRENZE) > 0) {
      return LiveRestEntscheidung.ausgesetztAb(g);
    }
    return LiveRestEntscheidung.angewandt(g, aktuelleStunde.plusHours(1));
  }

  /**
   * <b>G</b>: der Eimer, in dem der Lauf mit dem Wasserstand {@code wasserstand} lief — der Anfang
   * der Stunde vor W. Abgerundet, damit ein W abseits der vollen Stunde einen Eimer mehr abdeckt
   * und nie einen weniger; per Konstruktion ist W aber ein Stundenanfang.
   */
  static LocalDateTime laufeimer(LocalDateTime wasserstand) {
    return wasserstand.truncatedTo(ChronoUnit.HOURS).minusHours(1);
  }
}
